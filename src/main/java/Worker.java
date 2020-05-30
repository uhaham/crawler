import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker implements Runnable {

  private Logger log = Logger.getLogger(getClass().getName());


  private final Collection<String> visitedUrls;
  private final BlockingQueue<UrlEntity> urlQueue;

  private final WebReader webReader;
  private final EntityCounter domainCounter;
  private final EntityCounter concurrentDomainCounter;
  private final AtomicInteger concurrentExecutorsRunning;
  private final Map<String, Integer> pagesPerDomain;
  private final Status status;

  public Worker(final Collection<String> visitedUrls, final LinkedBlockingQueue<UrlEntity> urlQueue,
      final WebReader webReader, final EntityCounter domainCounter,
      final EntityCounter concurrentDomainCounter, final AtomicInteger concurrentExecutorsRunning,
      final Map<String, Integer> pagesPerDomain, final Status status) {
    this.visitedUrls = visitedUrls;
    this.urlQueue = urlQueue;
    this.webReader = webReader;
    this.domainCounter = domainCounter;
    this.concurrentDomainCounter = concurrentDomainCounter;
    this.concurrentExecutorsRunning = concurrentExecutorsRunning;
    this.pagesPerDomain = pagesPerDomain;
    this.status = status;
  }

  public void run() {

    log.info(String.format("[%d] Worker started", Thread.currentThread().getId()));

    while (!status.isCompleted()) {

      try {
        log.debug(String
            .format("[%d] reading from queue, status=%s", Thread.currentThread().getId(),
                status.isCompleted()));
        final UrlEntity urlEntity =
            urlQueue.poll(Config.WAIT_TIME_READING_FROM_QUEUE_MILLIS, TimeUnit.MILLISECONDS);
        final int val = concurrentExecutorsRunning.incrementAndGet();
        log.debug(String.format("[%d] increment, value =%d", Thread.currentThread().getId(), val));

        log.debug(String.format("[%d] read url=%s", Thread.currentThread().getId(),
            urlEntity != null ? urlEntity.getUrl() : null));
        handleUrl(urlEntity);
      } catch (InterruptedException e) {
      } catch (Exception e) {
        log.warn(String.format("[%d] worker error", Thread.currentThread().getId()), e);
      }
      final int val = concurrentExecutorsRunning.decrementAndGet();
      log.debug(String.format("[%d] decrement, value =%d", Thread.currentThread().getId(), val));
    }
    log.info(String.format("[%d] Worker Finished", Thread.currentThread().getId()));
  }

  protected void handleUrl(final UrlEntity urlEntity) throws Exception {

    if (!UrlValidator.isValidToDownload(urlEntity)) {
      log.debug(String.format("[%d] invalid, url=%s", Thread.currentThread().getId(),
          urlEntity != null ? urlEntity.getUrl() : "null"));
      return;
    }

    if (visitedUrls.contains(urlEntity.getUrl())) {
      log.debug(String.format("[%d] already visited, url=%s", Thread.currentThread().getId(),
          urlEntity.getUrl()));
      return;
    }

    final String domain = urlEntity.getDomain();

    final Integer maxPages = pagesPerDomain.get(domain);
    if (maxPages == null || domainCounter.get(domain) >= maxPages) {
      log.debug(String.format("[%d] domain pages quota is full, url=%s, domain=%s, quota=%d",
          Thread.currentThread().getId(), urlEntity.getUrl(), urlEntity.getDomain(), maxPages));
      return;
    }

    if (!concurrentDomainCounter
        .compareAndIncrease(domain, Config.WORKER_MAX_CONCURRENT_PAGES_PER_DOMAIN)) {
      log.debug(String.format(
          "[%d] too many workers on the same domain put the url back to the queue for later processing, url=%s",
          Thread.currentThread().getId(), urlEntity.getUrl()));
      urlQueue.put(urlEntity);
      return;
    }

    try {


      final String currentUrl = urlEntity.getUrl();
      final Document webPage = webReader.readWithRetry(currentUrl);
      final Set<UrlEntity> urls = WebUtils.getValidUrlEntities(currentUrl, webPage, visitedUrls);
      urlQueue.addAll(urls);
      if (domainCounter.get(domain) < pagesPerDomain.get(domain)) {
        WebUtils.write(urlEntity, webPage);
        domainCounter.increase(domain);
        visitedUrls.add(urlEntity.getUrl());
      }
    } catch (Exception e) {
      log.warn(String.format("[%d] ", Thread.currentThread().getId()), e);
    } finally {
      concurrentDomainCounter.decrease(domain);
    }
  }

}

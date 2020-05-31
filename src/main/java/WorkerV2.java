import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WorkerV2 {

  private Logger log = Logger.getLogger(getClass().getName());


  private final Collection<String> visitedUrls;
  private final BlockingQueue<UrlEntity> urlQueue;

  private final WebReader webReader;
  private final EntityCounter domainCounter;
  private final EntityCounter concurrentDomainCounter;
  private final Map<String, Integer> pagesPerDomain;

  public WorkerV2(final Collection<String> visitedUrls,
      final LinkedBlockingQueue<UrlEntity> urlQueue, final WebReader webReader,
      final EntityCounter domainCounter, final EntityCounter concurrentDomainCounter,
      final Map<String, Integer> pagesPerDomain) {
    this.visitedUrls = visitedUrls;
    this.urlQueue = urlQueue;
    this.webReader = webReader;
    this.domainCounter = domainCounter;
    this.concurrentDomainCounter = concurrentDomainCounter;
    this.pagesPerDomain = pagesPerDomain;
  }

  public void handleUrl(final UrlEntity urlEntity) {

    try {
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
        log.info(String.format("[%d] reading web page, url=%s", Thread.currentThread().getId(), currentUrl));
        final Document webPage = webReader.readWithRetry(currentUrl);
        final Set<UrlEntity> urls = WebUtils.getValidUrlEntities(currentUrl, webPage, visitedUrls);
        urlQueue.addAll(urls);
        if (domainCounter.get(domain) < pagesPerDomain.get(domain)) {
          WebUtils.write(urlEntity, webPage);
          domainCounter.increase(domain);
        }
      } catch (Exception e) {
        log.warn(String.format("[%d] ", Thread.currentThread().getId()), e);
      } finally {
        visitedUrls.add(urlEntity.getUrl());
        concurrentDomainCounter.decrease(domain);
      }
    } catch (InterruptedException e) {
    } catch (Exception e) {
      log.warn(String.format("[%d] worker error", Thread.currentThread().getId()), e);
    }
  }

}

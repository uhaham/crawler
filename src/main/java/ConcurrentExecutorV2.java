import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ConcurrentExecutorV2 {

  private Logger log = Logger.getLogger(getClass().getName());

  public void run(final String fileName) {

    log.info("Start ConcurrentExecutor");
    final Collection<String> visitedUrls = new ConcurrentHashMap<String,Object>().newKeySet();
    final EntityCounter domainCounter = new EntityCounter();
    final Map<String, Integer> pagesPerDomain = Maps.newHashMap();
    final EntityCounter concurrentDomainCounter = new EntityCounter();
    final LinkedBlockingQueue<UrlEntity> urlQueue = new LinkedBlockingQueue<>();

    log.info(String.format("clearing output directory=%s", WebUtils.BASE));
    WebUtils.deleteDirectory(new File(WebUtils.BASE));

    readInputAndSetIntoQueue(fileName, pagesPerDomain, urlQueue);

    final ThreadPoolExecutor executors =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(Config.NUMBER_OF_CONCURRENT_WORKERS);

    while (!urlQueue.isEmpty() || executors.getActiveCount() > 0) {
      log.debug(String.format("urlQueue size=%d, active executors=%d", urlQueue.size(), executors.getActiveCount()));
      try {
        final UrlEntity urlEntity =
            urlQueue.poll(Config.WAIT_TIME_READING_FROM_QUEUE_MILLIS, TimeUnit.MILLISECONDS);

        if (urlEntity != null && urlEntity.getUrl() != null) {
          final Consumer<UrlEntity> operation = urlEntityForOperation -> {
            final WorkerV2 worker =
                new WorkerV2(visitedUrls, urlQueue, new WebReader(), domainCounter,
                    concurrentDomainCounter, pagesPerDomain);
            worker.handleUrl(urlEntityForOperation);
          };

          Callable<Void> task = () -> {
            operation.accept(urlEntity);
            return null;
          };

          executors.submit(task);
        }

      } catch (InterruptedException e) {
      } catch (Exception e) {
        log.warn("error reading from queue", e);
      }
    }


    shutdown(executors);

    log.info("Finished ConcurrentExecutor");

  }

  private void shutdown(final ThreadPoolExecutor executors) {
    executors.shutdown();
    while (!executors.isShutdown()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // continue
      }
      executors.shutdownNow();
    }
  }

  private void readInputAndSetIntoQueue(final String fileName,
      final Map<String, Integer> pagesPerDomain, final LinkedBlockingQueue<UrlEntity> urlQueue) {
    BufferedReader csvReader = null;
    String row = null;
    try {
      csvReader = new BufferedReader(new FileReader(fileName));
      if (csvReader.readLine() != null) {
        // read header
        while ((row = csvReader.readLine()) != null) {
          String[] data = row.split(",");
          final String url = data[0];
          final String numberOfLinksAsString = data[1].trim();
          final Integer numberOfLinks = Integer.parseInt(numberOfLinksAsString);
          final UrlEntity urlEntity = new UrlEntity(url);
          pagesPerDomain.put(urlEntity.getDomain(), numberOfLinks);
          urlQueue.put(urlEntity);
        }
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (csvReader != null) {
        try {
          csvReader.close();
        } catch (IOException e) {
          log.error(String.format("line=%s", row), e);
          e.printStackTrace();
        }
      }
    }
  }

}





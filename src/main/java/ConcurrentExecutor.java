import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentExecutor {

  private Logger log = Logger.getLogger(getClass().getName());

  public void run(final String fileName) {

    log.info("Start ConcurrentExecutor");
    final Collection<String> visitedUrls = Sets.newHashSet();
    final EntityCounter domainCounter = new EntityCounter();
    final Map<String, Integer> pagesPerDomain = Maps.newHashMap();
    final EntityCounter concurrentDomainCounter = new EntityCounter();
    final LinkedBlockingQueue<UrlEntity> urlQueue = new LinkedBlockingQueue<>();
    final Status status = new Status();
    final AtomicInteger concurrentExecutorsRunning = new AtomicInteger(0);

    log.info(String.format("clearing output directory=%s", WebUtils.BASE));
    WebUtils.deleteDirectory(new File(WebUtils.BASE));

    final ExecutorService executor =
        Executors.newFixedThreadPool(Config.NUMBER_OF_CONCURRENT_WORKERS);

    log.info(String.format("spooning %d workers", Config.NUMBER_OF_CONCURRENT_WORKERS));
    for (int i = 0; i < Config.NUMBER_OF_CONCURRENT_WORKERS; i++) {
      final Worker worker =
          new Worker(visitedUrls, urlQueue, new WebReader(), domainCounter, concurrentDomainCounter,
              concurrentExecutorsRunning, pagesPerDomain, status);
      executor.submit(() -> worker.run());
    }

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

    while (!urlQueue.isEmpty() || concurrentExecutorsRunning.get() > 0) {
      log.debug(String.format("urlQueue=%d, concurrentExecutorsRunning=%d", urlQueue.size(),
          concurrentExecutorsRunning.get()));
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // continue
      }
    }

    log.debug(String.format("urlQueue=%d", urlQueue.size()));
    status.complete();
    log.debug(String.format("status=%s", status.isCompleted()));


    executor.shutdown();
    while (!executor.isShutdown()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // continue
      }
      executor.shutdownNow();
    }

    log.info("Finished ConcurrentExecutor");

  }

}





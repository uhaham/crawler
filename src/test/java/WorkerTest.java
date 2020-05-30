import com.github.rholder.retry.RetryException;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerTest {

  private static final String URL1 = "http://milk.com/";
  private static final UrlEntity URL_ENTITY = new UrlEntity(URL1);

  private final Worker worker;

  private final Collection<String> visitedUrls = Sets.newHashSet();
  private final EntityCounter domainCounter = new EntityCounter();
  private final Map<String, Integer> pagesPerDomain = Maps.newHashMap();
  private final EntityCounter concurrentDomainCounter = new EntityCounter();
  final LinkedBlockingQueue<UrlEntity> urlQueue = new LinkedBlockingQueue<>();


  public WorkerTest() throws Exception {
    worker = getWorker();
  }

  private Worker getWorker() throws ExecutionException, RetryException {
    final Document document = Jsoup.parse(HtmlTestHelper.getHtml());
    final WebReader webReader = Mockito.mock(WebReader.class);
    final Status status = new Status();
    Mockito.when(webReader.readWithRetry(Mockito.anyString())).thenReturn(document);
    return new Worker(visitedUrls, urlQueue, webReader, domainCounter, concurrentDomainCounter,
        new AtomicInteger(), pagesPerDomain, status);
  }

  @BeforeMethod
  public void setUp() {
    visitedUrls.clear();
    domainCounter.clear();
    pagesPerDomain.clear();
    concurrentDomainCounter.clear();
    urlQueue.clear();
  }

  @Test
  public void testHandleUrl() throws Exception {
    pagesPerDomain.put("milk.com", 4);
    worker.handleUrl(URL_ENTITY);
    validate(1, 1, 3);
  }

  @Test
  public void testHandleUrl_DomainQuoteIsFull() throws Exception {
    worker.handleUrl(URL_ENTITY);
    validate(1,0,0);
  }

  @Test
  public void testHandleUrl_VisitedUrl() throws Exception {
    pagesPerDomain.put("milk.com", 4);
    visitedUrls.add(URL1);
    worker.handleUrl(URL_ENTITY);
    validate(1,0,0);
  }

  @Test
  public void testHandleUrl_ConcurrentDomainOk() throws Exception {
    pagesPerDomain.put("milk.com", 4);
    concurrentDomainCounter.increase(URL_ENTITY.getDomain());
    worker.handleUrl(URL_ENTITY);
    validate(1, 1, 3);
  }

  @Test
  public void testHandleUrl_ConcurrentDomainNotOk() throws Exception {
    pagesPerDomain.put("milk.com", 4);
    concurrentDomainCounter.increase(URL_ENTITY.getDomain());
    concurrentDomainCounter.increase(URL_ENTITY.getDomain());
    worker.handleUrl(URL_ENTITY);
    validate(1,0,1);
  }

  private void validate(final int visited, final int domainCount, final int urlsInQueue) {
    Assert.assertEquals(visitedUrls.size(), visited);
    Assert.assertEquals(domainCounter.get(URL_ENTITY.getDomain()), domainCount);
    Assert.assertEquals(urlQueue.size(), urlsInQueue);
  }

}

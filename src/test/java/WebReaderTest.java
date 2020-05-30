import com.github.rholder.retry.RetryException;
import org.jsoup.nodes.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;

public class WebReaderTest {

  private static final String HTTP_URL = "http://milk.com/";
  private static final String HTTPS_URL = "https://www.tic.com/";

  @Test
  public void testReadWithRetry_Http() {
    final WebReader webReader = new WebReader();
    try {
      final Document doc = webReader.readWithRetry(HTTP_URL);
      Assert.assertNotNull(doc);
    } catch (ExecutionException | RetryException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReadWithRetry_Https() throws Exception {
    final WebReader webReader = new WebReader();
    final Document doc = webReader.readWithRetry(HTTPS_URL);
    Assert.assertNotNull(doc);
  }

}

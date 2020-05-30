import com.google.common.collect.Sets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public class WebUtilsTest {

  private static final UrlEntity URL_ENTITY1 = new UrlEntity("http://milk.com/");
  private static final UrlEntity URL_ENTITY2 =
      new UrlEntity("https://www.w3.org/History.html");
  private static final UrlEntity URL_ENTITY3 =
      new UrlEntity("https://www.w3.org/2004/Talks/w3c10-HowItAllStarted/");

  private static final Document DOC = Jsoup.parse(HtmlTestHelper.getHtml());

  @BeforeMethod
  public void setUp() {
    WebUtils.deleteDirectory(new File(WebUtils.BASE));
  }

  @Test(dataProvider = "params")
  public void testIsValid(final String url, final boolean expected) {
    Assert.assertEquals(WebUtils.isValid(url), expected);
  }

  @DataProvider()
  public static Object[][] params() {
    final ArrayList<Object[]> params = new ArrayList<>();
    params.add(new Object[] {null, false});
    params.add(new Object[] {"http://10.0.15.208:4040/abc/xyz", true});
    params.add(new Object[] {"http://itcorp.com/", true});
    params.add(new Object[] {"https://www.vortex.com/", true});
    params.add(new Object[] {"https://www.w3.org/History.html", true});
    params.add(new Object[] {"http://10.0.15.208:4040/abc/xyz", true});
    params.add(new Object[] {"https://10.0.15.208:4040/abc/xyz", true});
    params.add(new Object[] {"https://10.0.15.208/abc/xyz", true});
    params.add(new Object[] {"https://abc/xyz", true});
    params.add(new Object[] {"abc/xyz", false});
    params.add(new Object[] {"www.abc/xyz", false});
    return params.toArray(new Object[0][]);
  }

  @Test
  public void testWrite() throws IOException {
    final Set<String> expected = Sets.newHashSet("output/milk.com/http:__milk.com_",
        "output/w3.org/https:__www.w3.org_History.html",
        "output/w3.org/https:__www.w3.org_2004_Talks_w3c10-HowItAllStarted_");
    WebUtils.write(URL_ENTITY1, DOC);
    WebUtils.write(URL_ENTITY2, DOC);
    WebUtils.write(URL_ENTITY3, DOC);
    final Set<String> actual =
        Files.walk(Paths.get(WebUtils.BASE)).filter(Files::isRegularFile).map(Path::toString)
            .collect(Collectors.toSet());
    Assert.assertEquals(actual, expected);
  }

}

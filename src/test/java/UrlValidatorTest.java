import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;

public class UrlValidatorTest {

  private static final String NAME = "Just any name";
  
  final EntityCounter entityCounter = new EntityCounter();

  @Test(dataProvider = "params")
  public void testIsValidToDownload(final UrlEntity urlEntity, final boolean expected) {
    Assert.assertEquals(UrlValidator.isValidToDownload(urlEntity), expected);
  }

  @DataProvider()
  public static Object[][] params() {
    final ArrayList<Object[]> params = new ArrayList<>();
    params.add(new Object[] {null, false});
    params.add(new Object[] {new UrlEntity("http://10.0.15.208:4040/abc/xyz"), true});
    params.add(new Object[] {new UrlEntity("http://itcorp.com/"), true});
    params.add(new Object[] {new UrlEntity("https://www.vortex.com/"), true});
    params.add(new Object[] {new UrlEntity("https://www.w3.org/History.html"), true});
    params.add(new Object[] {new UrlEntity("http://10.0.15.208:4040/abc/xyz"), true});
    params.add(new Object[] {new UrlEntity("https://10.0.15.208:4040/abc/xyz"), true});
    params.add(new Object[] {new UrlEntity("https://10.0.15.208/abc/xyz"), true});
    params.add(new Object[] {new UrlEntity("https://abc/xyz"), true});
    params.add(new Object[] {new UrlEntity("abc/xyz"), false});
    params.add(new Object[] {new UrlEntity("www.abc/xyz"), false});
    params.add(new Object[] {new UrlEntity(null), false});
    return params.toArray(new Object[0][]);
  }

}

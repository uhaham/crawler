import com.sun.tools.javac.util.Pair;
import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class WebUtils {

  protected static final String BASE = "output";
  private static final String SEPARATOR = "/";

  public static void write(final UrlEntity urlEntity, final Document webPage) throws IOException {
    FileUtils.writeStringToFile(new File(
            BASE + SEPARATOR + urlEntity.getDomain() + SEPARATOR + encodeUrlToFileName(urlEntity.getUrl()) ),
        webPage.html(), "UTF-8", false);
  }

  public static boolean deleteDirectory(File directoryToBeDeleted) {
    final File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }

  private static String encodeUrlToFileName(final String url) {
    if (url == null)
    return null;
    return url.replaceAll("/","_");
  }

  public static String getDomainName(String url) throws URISyntaxException {
    if (url == null)
      return null;

    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain != null && domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  public static boolean isValid(String url) {
    if (url == null)
      return false;

    try {
      final URI uri = new URI(url);
      return uri.getHost() != null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  public static Set<UrlEntity> getValidUrlEntities(final String currentUrl, final Document webPage,
      final Collection<String> visitedUrls) {
    final Collection<UrlEntity> urlEntities = getUrlEntityLinks(webPage);
    return urlEntities.stream().filter(url -> WebUtils.isValid(url.getUrl()))
        .filter(newUrl -> !currentUrl.equals(newUrl.getUrl()))
        .filter(url -> !visitedUrls.contains(url.getUrl())).collect(Collectors.toSet());
  }

  private static Collection<UrlEntity> getUrlEntityLinks(final Document webPage) {
    return WebUtils.getLinks(webPage).stream().filter(pair -> WebUtils.isValid(pair.fst))
        .map(pair -> new UrlEntity(pair.fst)).collect(Collectors.toList());
  }

  private static Collection<Pair<String, String>> getLinks(final Document doc) {
    Elements links = doc.select("a[href]");
    return links.stream().map(link -> new Pair<>(link.attr("abs:href"), link.text().trim()))
        .collect(Collectors.toList());
  }

}


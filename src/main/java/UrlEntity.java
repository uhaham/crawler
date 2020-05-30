import java.net.URISyntaxException;

public class UrlEntity {

  final private String url;
  final private String domain;

  public UrlEntity(final String url) {
    this.url = url;
    this.domain = getDomainFromUrl(url);
    return;
  }

  public String getUrl() {
    return url;
  }

  public String getDomain() {
    return domain;
  }

  private String getDomainFromUrl(final String url) {
    try {
      return WebUtils.getDomainName(url);
    } catch (URISyntaxException e) {
      return null;
    }
  }

}

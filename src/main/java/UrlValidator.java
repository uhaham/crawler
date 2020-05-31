public class UrlValidator {


  public static boolean isValidToDownload(final UrlEntity urlEntity) {
    return urlEntity != null && urlEntity.getUrl() != null && urlEntity.getDomain() != null
        && !isMimeToBlock(urlEntity.getUrl());
  }

  protected static boolean isMimeToBlock(final String url) {
    if (url == null)
      return false;

    final String[] splitUrl = url.split("\\.");
    final String mime = splitUrl[splitUrl.length - 1];
    return mime.equals("gif") || mime.equals("jpg") || mime.equals("jpeg") || mime.equals("pdf");
  }

}

public class UrlValidator {


  public static boolean isValidToDownload(final UrlEntity urlEntity) {
    return urlEntity != null && urlEntity.getUrl() != null && urlEntity.getDomain() != null;
  }

}

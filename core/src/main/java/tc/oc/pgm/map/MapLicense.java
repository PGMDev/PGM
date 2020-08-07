package tc.oc.pgm.map;

public enum MapLicense {
  CC_BY("Attribution", "https://creativecommons.org/licenses/by/4.0/"),
  CC_BY_ND("Attribution-NoDerivs", "https://creativecommons.org/licenses/by-nd/4.0/"),
  CC_BY_SA("Attribution-ShareAlike", "https://creativecommons.org/licenses/by-sa/4.0/"),
  CC_BY_NC("Attribution-NonCommercial", "https://creativecommons.org/licenses/by-nc/4.0"),
  CC_BY_NC_ND(
      "Attribution-NonCommercial-NoDerivs", "https://creativecommons.org/licenses/by-nc-nd/4.0"),
  CC_BY_NC_SA(
      "Attribution-NonCommercial-ShareAlike", "https://creativecommons.org/licenses/by-nc-sa/4.0"),
  NONE("All Rights Reserved", "https://en.wikipedia.org/wiki/All_rights_reserved");

  private final String fullName;
  private final String URL;

  MapLicense(String fullName, String URL) {
    this.fullName = fullName;
    this.URL = URL;
  }

  public String getFullName() {
    return fullName;
  }

  public String getURL() {
    return URL;
  }
}

package tc.oc.pgm.map;

public enum MapLicense {
  CC_BY("Attribution 4.0 International"),
  CC_BY_ND("Attribution-NoDerivatives 4.0 International"),
  CC_BY_SA("Attribution-ShareAlike 4.0 International"),
  CC_BY_NC("Attribution-NonCommercial 4.0 International"),
  CC_BY_NC_ND("Attribution-NonCommercial-NoDerivatives 4.0 International"),
  CC_BY_NC_SA("Attribution-NonCommercial-ShareAlike 4.0 International"),
  CUSTOM(""),
  NONE("All Rights Reserved");

  private String fullName;

  MapLicense(String fullName) {
    this.fullName = fullName;
  }

  MapLicense setNameIfCustom(String customName) {
    if (this != MapLicense.CUSTOM)
      return this; // Not allowed to set custom if the map license is not the custom one
    this.fullName = customName;
    return this;
  }

  public String getFullName() {
    return fullName;
  }
}

package tc.oc.pgm.map;

public enum MapLicense {
  CC_BY("Attribution 4.0 International"),
  CC_BY_ND("Attribution-NoDerivatives 4.0 International"),
  CC_BY_SA("Attribution-ShareAlike 4.0 International"),
  CC_BY_NC("Attribution-NonCommercial 4.0 International"),
  CC_BY_NC_ND("Attribution-NonCommercial-NoDerivatives 4.0 International"),
  CC_BY_NC_SA("Attribution-NonCommercial-ShareAlike 4.0 International"),
  NONE("All Rights Reserved");

  String fullName;

  MapLicense(String fullName) {
    this.fullName = fullName;
  }

  ;

  public String getFullName() {
    return fullName;
  }
}

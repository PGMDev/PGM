package tc.oc.pgm.api.tracker.info;

public interface MeleeInfo extends PhysicalInfo, DamageInfo {
  PhysicalInfo weapon();

  @Deprecated
  default PhysicalInfo getWeapon() {
    return weapon();
  }
}

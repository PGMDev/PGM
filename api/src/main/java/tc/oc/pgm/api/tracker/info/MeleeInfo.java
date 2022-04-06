package tc.oc.pgm.api.tracker.info;

public interface MeleeInfo extends PhysicalInfo, DamageInfo {
  PhysicalInfo getWeapon();
}

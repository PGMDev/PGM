package tc.oc.pgm.tracker.damage;

import tc.oc.component.Component;

public interface PhysicalInfo extends OwnerInfo {
  String getIdentifier();

  Component getLocalizedName();
}

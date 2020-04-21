package tc.oc.pgm.api.tracker.info;

import tc.oc.pgm.util.component.Component;

public interface PhysicalInfo extends OwnerInfo {
  String getIdentifier();

  Component getLocalizedName();
}

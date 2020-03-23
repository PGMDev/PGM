package tc.oc.pgm.api.tracker.info;

import tc.oc.util.bukkit.component.Component;

public interface PhysicalInfo extends OwnerInfo {
  String getIdentifier();

  Component getLocalizedName();
}

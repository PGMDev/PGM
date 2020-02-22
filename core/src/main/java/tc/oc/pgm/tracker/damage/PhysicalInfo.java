package tc.oc.pgm.tracker.damage;

import tc.oc.util.bukkit.component.Component;

public interface PhysicalInfo extends OwnerInfo {
  String getIdentifier();

  Component getLocalizedName();
}

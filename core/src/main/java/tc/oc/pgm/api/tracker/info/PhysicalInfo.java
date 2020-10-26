package tc.oc.pgm.api.tracker.info;

import net.kyori.adventure.text.Component;

public interface PhysicalInfo extends OwnerInfo {
  String getIdentifier();

  Component getName();
}

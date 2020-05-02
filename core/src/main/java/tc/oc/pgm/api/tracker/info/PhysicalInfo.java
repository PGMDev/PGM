package tc.oc.pgm.api.tracker.info;

import net.kyori.text.Component;

public interface PhysicalInfo extends OwnerInfo {
  String getIdentifier();

  Component getName();
}

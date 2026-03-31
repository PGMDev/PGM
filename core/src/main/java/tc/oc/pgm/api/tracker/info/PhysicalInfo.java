package tc.oc.pgm.api.tracker.info;

import net.kyori.adventure.text.Component;

public interface PhysicalInfo extends OwnerInfo {
  String identifier();

  @Deprecated
  default String getIdentifier() {
    return identifier();
  }

  Component name();

  @Deprecated
  default Component getName() {
    return name();
  }
}

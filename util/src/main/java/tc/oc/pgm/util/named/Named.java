package tc.oc.pgm.util.named;

import net.kyori.adventure.text.Component;

public interface Named {

  Component getName(NameStyle style);

  default Component getName() {
    return getName(NameStyle.VERBOSE);
  }

  // TODO: Maybe add a note here explaining to prefer Named#getName()
  String getNameLegacy();
}

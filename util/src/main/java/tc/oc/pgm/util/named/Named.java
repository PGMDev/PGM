package tc.oc.pgm.util.named;

import net.kyori.text.Component;

public interface Named {

  Component getName(NameStyle style);

  Component getName();
}

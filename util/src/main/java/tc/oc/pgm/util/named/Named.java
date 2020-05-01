package tc.oc.pgm.util.named;

import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import tc.oc.pgm.util.component.Component;

public interface Named {

  /** @see #getName(NameStyle) */
  @Deprecated
  Component getStyledName(NameStyle style);

  default net.kyori.text.Component getName(NameStyle style) {
    return LegacyComponentSerializer.legacy()
        .deserialize(getStyledName(style).render().toLegacyText());
  }
}

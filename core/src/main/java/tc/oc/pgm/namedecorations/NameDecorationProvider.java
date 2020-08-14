package tc.oc.pgm.namedecorations;

import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;

/** This interface is intended to return what prefix and suffix a player should have */
public interface NameDecorationProvider {

  String getPrefix(UUID uuid);

  String getSuffix(UUID uuid);

  default Component getPrefixComponent(UUID uuid) {
    return TextComponent.of(getPrefix(uuid));
  }

  default Component getSuffixComponent(UUID uuid) {
    return TextComponent.of(getSuffix(uuid));
  }
}

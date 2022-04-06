package tc.oc.pgm.api.named;

import static net.kyori.adventure.text.Component.text;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** This interface is intended to return what prefix and suffix a player should have */
public interface NameDecorationProvider {

  String METADATA_KEY = "name-decoration-provider";

  NameDecorationProvider DEFAULT = new NoOpNameDecorationProvider();

  String getPrefix(UUID uuid);

  String getSuffix(UUID uuid);

  default TextColor getColor(UUID uuid) {
    return NamedTextColor.WHITE;
  }

  default Component getPrefixComponent(UUID uuid) {
    return text(getPrefix(uuid));
  }

  default Component getSuffixComponent(UUID uuid) {
    return text(getSuffix(uuid));
  }

  /** A No-op default decoration provider, used in the absence of a decoration provider */
  class NoOpNameDecorationProvider implements NameDecorationProvider {
    @Override
    public String getPrefix(UUID uuid) {
      return "";
    }

    @Override
    public String getSuffix(UUID uuid) {
      return "";
    }
  }
}

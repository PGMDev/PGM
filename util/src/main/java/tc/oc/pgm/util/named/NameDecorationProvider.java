package tc.oc.pgm.util.named;

import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

/** This interface is intended to return what prefix and suffix a player should have */
public interface NameDecorationProvider {

  String METADATA_KEY = "name-decoration-provider";

  NameDecorationProvider DEFAULT = new NoOpNameDecorationProvider();

  String getPrefix(UUID uuid);

  String getSuffix(UUID uuid);

  default TextColor getColor(UUID uuid) {
    return TextColor.WHITE;
  }

  default Component getPrefixComponent(UUID uuid) {
    return TextComponent.of(getPrefix(uuid));
  }

  default Component getSuffixComponent(UUID uuid) {
    return TextComponent.of(getSuffix(uuid));
  }

  /**
   * A No-op default decoration provider, used in the absence of a decoration provider
   */
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

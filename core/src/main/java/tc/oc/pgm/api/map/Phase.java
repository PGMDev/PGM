package tc.oc.pgm.api.map;

import static net.kyori.adventure.text.Component.translatable;

import java.util.Locale;
import net.kyori.adventure.text.Component;

public enum Phase {
  PRODUCTION,
  DEVELOPMENT;

  public Component toComponent() {
    return translatable("map.phase." + this.name().toLowerCase());
  }

  public static Phase of(String query) {
    query = query.toLowerCase(Locale.ROOT);
    for (Phase phase : Phase.values()) {
      if (phase.toString().toLowerCase(Locale.ROOT).startsWith(query)) {
        return phase;
      }
    }
    return null;
  }
}

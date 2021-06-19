package tc.oc.pgm.api.map;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;

public enum Phase {
  PRODUCTION,
  DEVELOPMENT;

  public Component toComponent() {
    return translatable("map.phase." + this.name().toLowerCase());
  }

  public static Phase of(String query) {
    for (Phase phase : Phase.values()) {
      if (phase.toString().toLowerCase().startsWith(query.toLowerCase())) {
        return phase;
      }
    }
    return null;
  }
}

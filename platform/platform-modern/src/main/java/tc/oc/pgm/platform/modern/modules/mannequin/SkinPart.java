package tc.oc.pgm.platform.modern.modules.mannequin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

public enum SkinPart {
  CAPE,
  JACKET,
  LEFT_SLEEVE,
  RIGHT_SLEEVE,
  LEFT_PANTS_LEG,
  RIGHT_PANTS_LEG,
  HAT;

  public static class SkinLayers {
    private final EnumSet<SkinPart> layers;

    private SkinLayers(EnumSet<SkinPart> layers) {
      this.layers = layers;
    }

    public boolean contains(SkinPart part) {
      return this.layers.contains(part);
    }

    public static SkinLayers of(SkinPart part) {
      return new SkinLayers(part == null ? EnumSet.noneOf(SkinPart.class) : EnumSet.of(part));
    }

    public static SkinLayers of(SkinPart... inputParts) {
      EnumSet<SkinPart> layers = EnumSet.noneOf(SkinPart.class);
      Collections.addAll(layers, inputParts);
      return new SkinLayers(layers);
    }

    public static SkinLayers allOf() {
      return new SkinLayers(EnumSet.allOf(SkinPart.class));
    }

    private static final Map<String, SkinLayers> ALIASES = Map.of(
        "all", SkinLayers.allOf(),
        "arms", SkinLayers.of(SkinPart.LEFT_SLEEVE, SkinPart.RIGHT_SLEEVE),
        "pants", SkinLayers.of(SkinPart.LEFT_PANTS_LEG, SkinPart.RIGHT_PANTS_LEG));

    public SkinLayers minus(SkinLayers other) {
      EnumSet<SkinPart> result = EnumSet.copyOf(this.layers);
      result.removeAll(other.layers);
      return new SkinLayers(result);
    }

    public static SkinLayers parse(String csv) {
      EnumSet<SkinPart> layers = EnumSet.noneOf(SkinPart.class);
      for (String token : csv.split(",")) {
        token = token.trim().toLowerCase(Locale.ROOT);
        SkinLayers alias = ALIASES.get(token);
        if (alias != null) {
          layers.addAll(alias.layers);
        } else {
          layers.add(SkinPart.valueOf(token.toUpperCase(Locale.ROOT)));
        }
      }
      return new SkinLayers(layers);
    }
  }
}

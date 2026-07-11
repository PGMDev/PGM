package tc.oc.pgm.mannequin;

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

  public static class SkinParts {
    private final EnumSet<SkinPart> parts;

    private SkinParts(EnumSet<SkinPart> parts) { this.parts = parts; }

    public boolean contains(SkinPart part) { return this.parts.contains(part); }

    public static SkinParts of(SkinPart part) {
      return new SkinParts(part == null ? EnumSet.noneOf(SkinPart.class) : EnumSet.of(part));
    }

    public static SkinParts of(SkinPart... inputParts) {
      EnumSet<SkinPart> parts = EnumSet.noneOf(SkinPart.class);
      Collections.addAll(parts, inputParts);
      return new SkinParts(parts);
    }

    public static SkinParts allOf() { return new SkinParts(EnumSet.allOf(SkinPart.class)); }

    public static SkinParts noneOf() { return new SkinParts(EnumSet.noneOf(SkinPart.class)); }

    private static final Map<String, SkinParts> ALIASES = Map.of(
        "all", SkinParts.allOf(),
        "arms", SkinParts.of(SkinPart.LEFT_SLEEVE, SkinPart.RIGHT_SLEEVE),
        "pants", SkinParts.of(SkinPart.LEFT_PANTS_LEG, SkinPart.RIGHT_PANTS_LEG)
    );

    public static SkinParts parse(String csv) {
      EnumSet<SkinPart> parts = EnumSet.noneOf(SkinPart.class);
      for (String token : csv.split(",")) {
        parts.add(SkinPart.valueOf(token.trim().toUpperCase(Locale.ROOT)));
      }
      return new SkinParts(parts);
    }
  }
}

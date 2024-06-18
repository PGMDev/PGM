package tc.oc.pgm.platform.v1_20_6.attribute;

import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/** Util to improve performance converting attributes between bukkit and PGM */
public abstract class AttributeUtilBukkit {

  private static final Map<Attribute, org.bukkit.attribute.Attribute> PGM_TO_BUKKIT =
      new EnumMap<>(Attribute.class);
  private static final Map<org.bukkit.attribute.Attribute, Attribute> BUKKIT_TO_PGM =
      new EnumMap<>(org.bukkit.attribute.Attribute.class);

  static {
    for (Attribute pgm : Attribute.values()) {
      try {
        var bukkit = org.bukkit.attribute.Attribute.valueOf(pgm.name());
        PGM_TO_BUKKIT.put(pgm, bukkit);
        BUKKIT_TO_PGM.put(bukkit, pgm);
      } catch (IllegalArgumentException e) {
        BukkitUtils.getPlugin().getLogger().warning("Attribute not found: " + pgm.name());
      }
    }
  }

  public static org.bukkit.attribute.Attribute toBukkit(Attribute attribute) {
    return PGM_TO_BUKKIT.get(attribute);
  }

  public static Attribute fromBukkit(org.bukkit.attribute.Attribute attribute) {
    return BUKKIT_TO_PGM.get(attribute);
  }

  @NotNull
  public static AttributeModifier fromBukkit(org.bukkit.attribute.AttributeModifier modifier) {
    return new AttributeModifier(
        modifier.getUniqueId(),
        modifier.getName(),
        modifier.getAmount(),
        AttributeModifier.Operation.values()[modifier.getOperation().ordinal()]);
  }

  @NotNull
  public static org.bukkit.attribute.AttributeModifier toBukkit(AttributeModifier modifier) {

    return new org.bukkit.attribute.AttributeModifier(
        modifier.getUniqueId(),
        modifier.getName(),
        modifier.getAmount(),
        org.bukkit.attribute.AttributeModifier.Operation.values()[
            modifier.getOperation().ordinal()]);
  }
}

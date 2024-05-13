package tc.oc.pgm.platform.attribute;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeModifier;

/** Util to improve performance converting attributes between bukkit and PGM */
public interface AttributeUtilBukkit {

  Map<Attribute, org.bukkit.attribute.Attribute> attributeCacheToBukkit =
      new EnumMap<>(Attribute.class);
  Map<org.bukkit.attribute.Attribute, Attribute> attributeCacheToPGM =
      new EnumMap<>(org.bukkit.attribute.Attribute.class);

  EnumSet<Attribute> missingAttributes = findMissingAttributes();

  static EnumSet<Attribute> findMissingAttributes() {
    Set<Attribute> missingAttributes = new HashSet<>();

    for (Attribute attribute : Attribute.values()) {
      try {
        org.bukkit.attribute.Attribute.valueOf(attribute.name());
      } catch (Exception e) {
        missingAttributes.add(attribute);
      }
    }

    return missingAttributes.isEmpty()
        ? EnumSet.noneOf(Attribute.class)
        : EnumSet.copyOf(missingAttributes);
  }

  static org.bukkit.attribute.Attribute convertAttribute(Attribute attribute) {
    if (missingAttributes.contains(attribute)) return null;
    org.bukkit.attribute.Attribute cachedAttribute = attributeCacheToBukkit.get(attribute);
    if (cachedAttribute == null) {
      cachedAttribute = org.bukkit.attribute.Attribute.valueOf(attribute.name());
      attributeCacheToBukkit.put(attribute, cachedAttribute);
    }
    return cachedAttribute;
  }

  static Attribute convertAttribute(org.bukkit.attribute.Attribute attribute) {
    Attribute cachedAttribute = attributeCacheToPGM.get(attribute);
    if (cachedAttribute == null) {
      cachedAttribute = Attribute.valueOf(attribute.name());
      attributeCacheToPGM.put(attribute, cachedAttribute);
    }
    return cachedAttribute;
  }

  @NotNull
  static AttributeModifier convertAttributeModifier(
      org.bukkit.attribute.AttributeModifier modifier) {
    return new AttributeModifier(
        modifier.getUniqueId(),
        modifier.getName(),
        modifier.getAmount(),
        AttributeModifier.Operation.values()[modifier.getOperation().ordinal()]);
  }

  @NotNull
  static org.bukkit.attribute.AttributeModifier convertAttributeModifier(
      AttributeModifier modifier) {

    return new org.bukkit.attribute.AttributeModifier(
        modifier.getUniqueId(),
        modifier.getName(),
        modifier.getAmount(),
        org.bukkit.attribute.AttributeModifier.Operation.values()[
            modifier.getOperation().ordinal()]);
  }
}

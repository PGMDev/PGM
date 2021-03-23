package tc.oc.pgm.util.compatability;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public interface SportAttributeUtils {
  Attribute[] ATTRIBUTES = Attribute.values();

  static void clearAttributes(Player player) {
    for (Attribute attribute : ATTRIBUTES) {
      AttributeInstance attributes = player.getAttribute(attribute);
      if (attributes == null) continue;

      for (AttributeModifier modifier : attributes.getModifiers()) {
        attributes.removeModifier(modifier);
      }
    }
  }
}

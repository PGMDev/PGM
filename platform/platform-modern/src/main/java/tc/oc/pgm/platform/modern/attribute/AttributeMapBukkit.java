package tc.oc.pgm.platform.modern.attribute;

import org.bukkit.entity.Player;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeMap;

public class AttributeMapBukkit implements AttributeMap {
  private final Player player;

  public AttributeMapBukkit(Player player) {
    this.player = player;
  }

  @Override
  public AttributeInstance getAttribute(Attribute attribute) {
    var bukkitAttribute = AttributeUtilBukkit.toBukkit(attribute);
    if (bukkitAttribute == null) return null;
    org.bukkit.attribute.AttributeInstance attributeInstance = player.getAttribute(bukkitAttribute);
    return attributeInstance == null ? null : new AttributeInstanceBukkit(attributeInstance);
  }
}

package tc.oc.pgm.util.nms.attribute;

import org.bukkit.entity.Player;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeMap;

public class AttributeMapBukkit implements AttributeMap {
  private Player player;

  public AttributeMapBukkit(Player player) {
    this.player = player;
  }

  @Override
  public AttributeInstance getAttribute(Attribute attribute) {
    org.bukkit.attribute.Attribute bukkitAttribute =
        AttributeUtilBukkit.convertAttribute(attribute);
    if (bukkitAttribute == null) return null;
    org.bukkit.attribute.AttributeInstance attributeInstance = player.getAttribute(bukkitAttribute);
    return attributeInstance == null ? null : new AttributeInstanceBukkit(attributeInstance);
  }
}

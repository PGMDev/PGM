package tc.oc.pgm.util.attribute;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.minecraft.server.v1_8_R3.AttributeMapBase;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class AttributeMapImpl implements AttributeMap {

  private final AttributeMapBase handle;

  public AttributeMapImpl(Player player) {
    handle = ((CraftPlayer) player).getHandle().getAttributeMap();
  }

  public AttributeMapImpl(AttributeMapBase handle) {
    this.handle = handle;
  }

  @Override
  public AttributeInstance getAttribute(Attribute attribute) {
    assertNotNull(attribute, "attribute");
    net.minecraft.server.v1_8_R3.AttributeInstance nms = handle.a(toMinecraft(attribute.name()));

    return (nms == null) ? null : new AttributeInstanceImpl(nms, attribute);
  }

  static String toMinecraft(String bukkit) {
    int first = bukkit.indexOf('_');
    int second = bukkit.indexOf('_', first + 1);

    StringBuilder sb = new StringBuilder(bukkit.toLowerCase(java.util.Locale.ENGLISH));

    sb.setCharAt(first, '.');
    if (second != -1) {
      sb.deleteCharAt(second);
      sb.setCharAt(second, bukkit.charAt(second + 1));
    }

    return sb.toString();
  }
}

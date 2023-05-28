package tc.oc.pgm.util.nms.attribute;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.minecraft.server.v1_8_R3.AttributeMapBase;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeMap;

public class AttributeMap1_8 implements AttributeMap {

  private final AttributeMapBase handle;

  public AttributeMap1_8(Player player) {
    handle = ((CraftPlayer) player).getHandle().getAttributeMap();
  }

  @Override
  public AttributeInstance getAttribute(Attribute attribute) {
    assertNotNull(attribute, "attribute");
    net.minecraft.server.v1_8_R3.AttributeInstance nms = handle.a(toMinecraft(attribute.name()));

    return (nms == null) ? null : new AttributeInstance1_8(nms, attribute);
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

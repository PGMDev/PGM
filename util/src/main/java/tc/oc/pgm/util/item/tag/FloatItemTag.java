package tc.oc.pgm.util.item.tag;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import tc.oc.pgm.util.item.ItemTag;

public class FloatItemTag extends ItemTag<Float> {

  public FloatItemTag(String name, Float defaultValue) {
    super(name, defaultValue);
  }

  @Override
  protected boolean hasPrimitive(NBTTagCompound tag) {
    return tag.hasKeyOfType(name, 5);
  }

  @Override
  protected Float getPrimitive(NBTTagCompound tag) {
    return tag.getFloat(name);
  }

  @Override
  protected void setPrimitive(NBTTagCompound tag, Float value) {
    tag.setFloat(name, value);
  }
}

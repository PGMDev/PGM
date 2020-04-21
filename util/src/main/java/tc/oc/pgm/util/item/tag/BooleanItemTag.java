package tc.oc.pgm.util.item.tag;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import tc.oc.pgm.util.item.ItemTag;

public class BooleanItemTag extends ItemTag<Boolean> {

  public BooleanItemTag(String name, Boolean defaultValue) {
    super(name, defaultValue);
  }

  @Override
  protected boolean hasPrimitive(NBTTagCompound tag) {
    return tag.hasKeyOfType(name, 1);
  }

  @Override
  protected Boolean getPrimitive(NBTTagCompound tag) {
    return tag.getBoolean(name);
  }

  @Override
  protected void setPrimitive(NBTTagCompound tag, Boolean value) {
    tag.setBoolean(name, value);
  }
}

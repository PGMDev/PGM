package tc.oc.util.bukkit.item.tag;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import tc.oc.util.bukkit.item.ItemTag;

public class StringItemTag extends ItemTag<String> {

  public StringItemTag(String name, String defaultValue) {
    super(name, defaultValue);
  }

  @Override
  protected boolean hasPrimitive(NBTTagCompound tag) {
    return tag.hasKeyOfType(name, 8);
  }

  @Override
  protected String getPrimitive(NBTTagCompound tag) {
    return tag.getString(name);
  }

  @Override
  protected void setPrimitive(NBTTagCompound tag, String value) {
    tag.setString(name, value);
  }
}

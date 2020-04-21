package tc.oc.pgm.util.item;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.inventory.ItemStack;

public abstract class ItemTag<T> {

  protected final String name;
  protected final T defaultValue;

  protected ItemTag(String name, T defaultValue) {
    this.name = name;
    this.defaultValue = defaultValue;
  }

  protected abstract boolean hasPrimitive(NBTTagCompound tag);

  protected abstract T getPrimitive(NBTTagCompound tag);

  protected abstract void setPrimitive(NBTTagCompound tag, T value);

  protected void clearPrimitive(NBTTagCompound tag) {
    tag.remove(name);
  }

  public boolean has(@Nullable NBTTagCompound tag) {
    return tag != null && hasPrimitive(tag);
  }

  public T get(@Nullable NBTTagCompound tag) {
    if (tag != null && hasPrimitive(tag)) {
      return getPrimitive(tag);
    } else {
      return defaultValue;
    }
  }

  public void set(NBTTagCompound tag, T value) {
    setPrimitive(tag, Preconditions.checkNotNull(value));
  }

  public void clear(@Nullable NBTTagCompound tag) {
    if (tag != null) clearPrimitive(tag);
  }

  public boolean has(ItemStack stack) {
    return has(NBTUtils.getCustomTag(stack));
  }

  public T get(ItemStack stack) {
    return get(NBTUtils.getCustomTag(stack));
  }

  public void set(ItemStack stack, T value) {
    set(NBTUtils.getOrCreateCustomTag(stack), value);
  }

  public ItemStack force(ItemStack stack, T value) {
    stack = NBTUtils.toCraft(stack);
    set(stack, value);
    return stack;
  }

  public void clear(ItemStack stack) {
    clear(NBTUtils.getCustomTag(stack));
  }
}

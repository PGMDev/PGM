package tc.oc.item;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

/*
 * This class allows for storage of arbitrary NBT data on items, under the key "PGM_DATA"
 */
public interface NBTUtils {

  String KEY = "PGM_DATA";

  /** Return the root tag, or null if the stack does not have a tag */
  static @Nullable NBTTagCompound getRootTag(ItemStack stack) {
    if (!(stack instanceof CraftItemStack)) return null;
    net.minecraft.server.v1_8_R3.ItemStack nmsStack = ((CraftItemStack) stack).getHandle();
    return nmsStack == null ? null : nmsStack.getTag();
  }

  /** Return the private namespace tag, or null if it is not present in the stack */
  static @Nullable NBTTagCompound getCustomTag(ItemStack stack) {
    NBTTagCompound tag = getRootTag(stack);
    if (tag != null && tag.hasKeyOfType(KEY, 10)) {
      return tag.getCompound(KEY);
    } else {
      return null;
    }
  }

  /** Return the root tag, creating it if it does not exist */
  static NBTTagCompound getOrCreateRootTag(ItemStack stack) {
    Preconditions.checkState(
        stack instanceof CraftItemStack, "stack must be CraftItemStack to store NBT");

    net.minecraft.server.v1_8_R3.ItemStack nmsStack = ((CraftItemStack) stack).getHandle();

    NBTTagCompound tag = nmsStack.getTag();
    if (tag == null) {
      tag = new NBTTagCompound();
      nmsStack.setTag(tag);
    }

    return tag;
  }

  /** Return the private namespace tag, creating it if it does not exist */
  static NBTTagCompound getOrCreateCustomTag(ItemStack stack) {
    NBTTagCompound root = getOrCreateRootTag(stack);
    if (!root.hasKeyOfType(KEY, 10)) {
      root.set(KEY, new NBTTagCompound());
    }
    return root.getCompound(KEY);
  }

  /** Return a possibly different stack that is guaranteed to support custom NBT */
  static ItemStack toCraft(ItemStack stack) {
    if (stack instanceof CraftItemStack) {
      return stack;
    } else {
      return CraftItemStack.asCraftCopy(stack);
    }
  }
}

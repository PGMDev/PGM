package tc.oc.pgm.util.inventory.tag;

import com.cryptomorin.xseries.XItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ModernItemTag<T> implements ItemTag<T> {

  NamespacedKey key;

  public ModernItemTag(NamespacedKey key) {
    this.key = key;
  }

  @Override
  public T get(ItemStack item) {
    return item.getItemMeta()
        .getPersistentDataContainer()
        .get(key, new PersistentDataType.PrimitivePersistentDataType(Class<T>.getClass()));
  }

  @Override
  public boolean has(ItemStack item) {
    return ItemTag.super.has(item);
  }

  @Override
  public void set(ItemStack item, Object value) {

  }

  @Override
  public void clear(ItemStack item) {

  }
}

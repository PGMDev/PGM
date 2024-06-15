package tc.oc.pgm.platform.v1_20_6.inventory;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/** An item tag that encodes data in an item meta's persistent data container */
final class ModernItemTag<T> implements ItemTag<T> {

  private final NamespacedKey key;
  private final PersistentDataType<?, T> type;

  ModernItemTag(String key, PersistentDataType<?, T> type) {
    this.key = NamespacedKey.fromString(key, BukkitUtils.getPlugin());
    this.type = type;
  }

  @Nullable
  @Override
  public T get(ItemStack item) {
    if (!item.hasItemMeta()) return null;
    return item.getItemMeta().getPersistentDataContainer().get(key, type);
  }

  @Override
  public void set(ItemStack item, T value) {
    ItemMeta itemMeta = item.getItemMeta();
    if (!item.hasItemMeta()) {
      // Create missing item meta if none is found
      itemMeta = Bukkit.getItemFactory().getItemMeta(item.getType());
    }
    itemMeta.getPersistentDataContainer().set(key, type, value);
    item.setItemMeta(itemMeta);
  }

  @Override
  public void clear(ItemStack item) {
    if (!item.hasItemMeta()) return;
    ItemMeta itemMeta = item.getItemMeta();
    itemMeta.getPersistentDataContainer().remove(key);
    item.setItemMeta(itemMeta);
  }
}

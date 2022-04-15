package tc.oc.pgm.util.inventory.tag;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class StringItemTag implements ItemTag<String> {

  NamespacedKey key;

  public StringItemTag(NamespacedKey key) {
    this.key = key;
  }

  @Override
  public String get(ItemStack item) {
    return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
  }

  @Override
  public boolean has(ItemStack item) {
    return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
  }

  @Override
  public void set(ItemStack item, String value) {
    ItemMeta itemMeta = item.getItemMeta();
    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
    item.setItemMeta(itemMeta);
  }

  @Override
  public void clear(ItemStack item) {
    item.getItemMeta().getPersistentDataContainer().remove(key);
  }
}

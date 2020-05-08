package tc.oc.pgm.util.inventory;

import java.util.Arrays;
import net.kyori.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.text.TextTranslations;

/** A nice way to build {@link ItemStack}s. */
public class ItemBuilder {

  protected final ItemStack item;
  private ItemMeta meta;

  public ItemBuilder() {
    this(new ItemStack(Material.AIR));
  }

  public ItemBuilder(ItemStack item) {
    this.item = item;
    this.meta = item.getItemMeta();
  }

  protected ItemMeta meta() {
    return meta == null ? meta = item.getItemMeta() : meta;
  }

  public ItemStack build() {
    item.setItemMeta(meta);
    return item;
  }

  public ItemStack copy() {
    return build().clone();
  }

  public ItemBuilder material(Material material) {
    item.setType(material);
    return this;
  }

  public ItemBuilder material(MaterialData material) {
    item.setType(material.getItemType());
    item.setData(material);
    return this;
  }

  public ItemBuilder amount(int amount) {
    item.setAmount(amount);
    return this;
  }

  public ItemBuilder durability(int durability) {
    item.setDurability((short) durability);
    return this;
  }

  public ItemBuilder name(String name) {
    meta().setDisplayName(name);
    return this;
  }

  public ItemBuilder name(CommandSender sender, Component name) {
    return name(TextTranslations.translateLegacy(name, sender));
  }

  public ItemBuilder lore(String... lore) {
    meta().setLore(Arrays.asList(lore));
    return this;
  }

  public ItemBuilder lore(CommandSender sender, Component... lore) {
    String[] loreStrings = new String[lore.length];
    for (int i = 0; i < lore.length; i++) {
      loreStrings[i] = TextTranslations.translateLegacy(lore[i], sender);
    }

    return lore(loreStrings);
  }

  public ItemBuilder flags(ItemFlag... flags) {
    meta().addItemFlags(flags);
    return this;
  }

  public ItemBuilder unbreakable(boolean unbreakable) {
    meta().spigot().setUnbreakable(unbreakable);
    return this;
  }

  public ItemBuilder enchant(Enchantment enchantment, int level) {
    meta().addEnchant(enchantment, level, true);
    return this;
  }
}

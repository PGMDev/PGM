package tc.oc.pgm.util.inventory;

import com.google.common.collect.Lists;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.jetbrains.annotations.Nullable;
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

  @Deprecated
  public ItemBuilder name(String name) {
    meta().setDisplayName(name);
    return this;
  }

  public ItemBuilder name(@Nullable CommandSender viewer, Component component) {
    meta().setDisplayName(TextTranslations.translateLegacy(component, viewer));
    return this;
  }

  @Deprecated
  public ItemBuilder lore(String... lore) {
    meta().setLore(Arrays.asList(lore));
    return this;
  }

  public ItemBuilder lore(@Nullable CommandSender viewer, Component... lore) {
    meta()
        .setLore(
            Lists.transform(
                Arrays.asList(lore),
                (component) -> TextTranslations.translateLegacy(component, viewer)));
    return this;
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

  public ItemBuilder color(DyeColor color) {
    final Material type = item.getType();
    switch (type) {
      case INK_SACK:
        item.setData(new Dye(color));
        break;

      case WOOL:
        item.setData(new Wool(color));
        break;

      default:
        // banners/other colored blocks
        item.setData(new MaterialData(type, color.getWoolData()));
        break;
    }
    item.setDurability(item.getData().getData());
    return this;
  }
}

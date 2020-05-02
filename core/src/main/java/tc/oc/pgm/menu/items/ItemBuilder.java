package tc.oc.pgm.menu.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;

/**
 * Builder utility class that is used to help and simplify the creation of creating an {@link
 * ItemStack}
 */
public class ItemBuilder {
  private ItemStack itemStack;

  private ItemBuilder(Material material) {
    this.itemStack = new ItemStack(material);
  }

  /**
   * Creates a new {@link ItemBuilder} for the specific {@link Material}
   *
   * @param material the material of the item builder
   * @return the created {@link ItemBuilder}
   */
  public static ItemBuilder of(Material material) {
    return new ItemBuilder(material);
  }

  /**
   * Sets the durability of the {@link ItemStack}
   *
   * @param durability the durability to set
   * @return the builder
   */
  public ItemBuilder setDurability(short durability) {
    itemStack.setDurability(durability);
    return this;
  }

  /**
   * Manipulates the {@link MaterialData} of the {@link ItemStack}
   *
   * @param dataConsumer the function that modifies the data
   * @return the builder
   */
  public ItemBuilder manipulateData(Consumer<MaterialData> dataConsumer) {
    MaterialData data = itemStack.getData();
    dataConsumer.accept(data);
    itemStack.setData(data);
    return this;
  }

  /**
   * Sets the name of the {@link ItemStack}
   *
   * @param name the name
   * @return the builder
   */
  public ItemBuilder setName(String name) {
    manipulateMeta(x -> x.setDisplayName(name));
    return this;
  }

  /**
   * Manipulates the {@link ItemMeta} of the {@link ItemStack} by running it through a lambda
   * argument
   *
   * @param metaConsumer the lambda argument manipulating the {@link ItemMeta}
   * @return the builder
   */
  public ItemBuilder manipulateMeta(Consumer<ItemMeta> metaConsumer) {
    ItemMeta meta = itemStack.getItemMeta();
    metaConsumer.accept(meta);
    itemStack.setItemMeta(meta);
    return this;
  }

  /**
   * Adds an {@link Enchantment} to the {@link ItemStack}
   *
   * @param enchantment the enchantment to add
   * @param level the level of the enchantment to add
   * @return the builder
   */
  public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
    itemStack.addUnsafeEnchantment(enchantment, level);
    return this;
  }

  /**
   * Set's the owner of skull, if the item is a skull
   *
   * @param owner the owner of the skull
   * @return the builder
   */
  public ItemBuilder setSkullString(String owner) {
    manipulateMeta(
        x -> {
          if (x instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) x;
            skull.setOwner(owner);
          }
        });
    return this;
  }

  /**
   * Sets the lore of the item
   *
   * @param lore a list of lores
   * @return the builder
   */
  public ItemBuilder setLore(List<String> lore) {
    manipulateMeta(
        x -> {
          x.setLore(lore);
        });
    return this;
  }

  /**
   * Adds a line of text to the lore
   *
   * @param lore the lore line
   * @return the builder
   */
  public ItemBuilder addLore(String lore) {
    manipulateMeta(
        x -> {
          x.getLore().add(lore);
        });
    return this;
  }

  /**
   * Sets the lore to a {@link Component}, useful when dealing with localized lore
   *
   * @param lore the lore array
   * @param player the player to localize the text to
   * @return the builder
   */
  public ItemBuilder setLore(Component[] lore, MatchPlayer player) {
    setLore(Collections.emptyList()); // clear the lore
    for (Component line : lore) {
      addLore(ComponentRenderers.toLegacyText(line, player.getBukkit()));
    }
    return this;
  }

  /**
   * Sets the lore to a vararg of strings
   *
   * @param lore the lore
   * @return the builder
   */
  public ItemBuilder setLore(String... lore) {
    return setLore(Arrays.asList(lore));
  }

  /**
   * Constructs an item stack from the builder
   *
   * @return the constructed item stack
   */
  public ItemStack stack() {
    return itemStack;
  }
}

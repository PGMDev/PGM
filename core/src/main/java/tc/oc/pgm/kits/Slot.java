package tc.oc.pgm.kits;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.util.inventory.InventoryUtils;

/**
 * Derived from the names found in net.minecraft.server.CommandReplaceItem. If we ever implement
 * applying kits to other types of inventories, this should be expanded to include those slot names
 * as well.
 */
public abstract class Slot {

  private final String key;
  private final int index; // -1 = auto

  private Slot(String key, int index) {
    this.key = key;
    this.index = index;

    byKey.put(key, this);
  }

  /** @return the name of this slot, as used by the /replaceitem command */
  public String getKey() {
    return "slot." + key;
  }

  /** @return a slot index that can be passed to {@link Inventory#getItem} et al. */
  public int getIndex() {
    if (isAuto()) throw new UnsupportedOperationException("The auto-slot has no index");
    return index;
  }

  public Range<Integer> getIndexRange() {
    if (isAuto()) {
      return getAutoIndexRange();
    } else {
      return Range.singleton(getIndex());
    }
  }

  protected abstract Range<Integer> getAutoIndexRange();

  /**
   * @return true if this is the special "auto" slot. Putting a stack in this slot will merge it
   *     with the inventory by calling {@link Inventory#addItem}
   */
  public boolean isAuto() {
    return index < 0;
  }

  // If we support more inventory types, we would probably widen the type accepted by this method
  public abstract Inventory getInventory(HumanEntity holder);

  protected ItemStack addItem(HumanEntity holder, ItemStack stack) {
    return InventoryUtils.placeStack(
        getInventory(holder),
        ContiguousSet.create(getAutoIndexRange(), DiscreteDomain.integers()),
        stack);
  }

  /**
   * @return the item in this slot of the given holder's inventory, or null if the slot is empty.
   *     This will never return a stack of {@link Material#AIR}.
   */
  public @Nullable ItemStack getItem(HumanEntity holder) {
    ItemStack stack = getInventory(holder).getItem(getIndex());
    return stack == null || stack.getType() == Material.AIR ? null : stack;
  }

  /**
   * Put the given stack in this slot of the given holder's inventory.
   *
   * @return a stack of any items that were NOT placed in the inventory, or null if the entire stack
   *     was placed. This can only be non-null when placing in the auto-slot.
   */
  public @Nullable ItemStack putItem(HumanEntity holder, ItemStack stack) {
    if (isAuto()) {
      stack = addItem(holder, stack);
      return stack.getAmount() > 0 ? stack : null;
    } else {
      getInventory(holder).setItem(getIndex(), stack);
      return null;
    }
  }

  public static class Player extends Slot {
    protected Player(String key, int index) {
      super(key, index);

      byIndex.put(index, this);
    }

    @Override
    public Inventory getInventory(HumanEntity holder) {
      return holder.getInventory();
    }

    @Override
    protected Range<Integer> getAutoIndexRange() {
      return Range.closed(0, 39);
    }

    /**
     * Convert a {@link PlayerInventory} slot index to a {@link Slot} object. Returns null if the
     * index is out of range.
     */
    public static @Nullable Slot forIndex(int index) {
      return byIndex.get(index);
    }
  }

  public static class Hotbar extends Player {
    protected Hotbar(String key, int index) {
      super(key, index);
    }

    @Override
    protected Range<Integer> getAutoIndexRange() {
      return Range.closed(0, 8);
    }
  }

  public static class Pockets extends Player {
    protected Pockets(String key, int index) {
      super(key, index);
    }

    @Override
    protected Range<Integer> getAutoIndexRange() {
      return Range.closed(9, 35);
    }
  }

  public static class Armor extends Player {
    private final ArmorType armorType;

    protected Armor(String key, int index, ArmorType armorType) {
      super(key, index);
      this.armorType = armorType;
      byArmorType.put(armorType, this);
    }

    @Override
    protected Range<Integer> getAutoIndexRange() {
      return Range.closed(36, 39);
    }

    public ArmorType getArmorType() {
      return armorType;
    }

    public static Armor forType(ArmorType armorType) {
      return byArmorType.get(armorType);
    }
  }

  public static class EnderChest extends Slot {
    protected EnderChest(String key, int index) {
      super(key, index);
    }

    @Override
    public Inventory getInventory(HumanEntity holder) {
      return holder.getEnderChest();
    }

    @Override
    protected Range<Integer> getAutoIndexRange() {
      return Range.closed(0, 26);
    }
  }

  private static final Map<String, Slot> byKey = new HashMap<>();
  private static final Map<Integer, Player> byIndex = new HashMap<>();
  private static final Map<ArmorType, Armor> byArmorType = new EnumMap<>(ArmorType.class);

  /**
   * Convert a Mojang slot name (used by /replaceitem) to a {@link Slot} object. The "slot." at the
   * beginning of the name is optional. Returns null if the name is invalid.
   */
  public static @Nullable Slot forKey(String key) {
    if (key.startsWith("slot.")) {
      key = key.substring("slot.".length());
    }
    return byKey.get(key);
  }

  static {
    // new Hotbar("hotbar", -1);
    for (int i = 0; i < 9; i++) {
      new Hotbar("hotbar." + i, i);
    }

    // new Pockets("inventory", -1);
    // new EnderChest("enderchest", -1);
    for (int i = 0; i < 27; i++) {
      new Pockets("inventory." + i, 9 + i);
      new EnderChest("enderchest." + i, i);
    }

    new Armor("armor.feet", 36, ArmorType.BOOTS);
    new Armor("armor.legs", 37, ArmorType.LEGGINGS);
    new Armor("armor.chest", 38, ArmorType.CHESTPLATE);
    new Armor("armor.head", 39, ArmorType.HELMET);
  }
}

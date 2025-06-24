package tc.oc.pgm.util.inventory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.platform.Platform;

/**
 * Derived from the names found in net.minecraft.server.CommandReplaceItem. If we ever implement
 * applying kits to other types of inventories, this should be expanded to include those slot names
 * as well.
 */
public abstract class Slot {
  static {
    all = new HashSet<>();
    byKey = new HashMap<>();
    byIndex = HashBasedTable.create();
    byInventoryType = ImmutableMap.<Class<? extends Inventory>, Class<? extends Slot>>builder()
        .put(PlayerInventory.class, Player.class)
        .put(Inventory.class, Container.class)
        .build();

    Container.init();
    Player.init();
    EnderChest.init();
  }

  private static final Set<Slot> all;
  private static final Map<String, Slot> byKey;
  private static final Table<Class<? extends Slot>, Integer, Slot> byIndex;
  private static final Map<Class<? extends Inventory>, Class<? extends Slot>> byInventoryType;

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

  public static @Nullable <S extends Slot> S forIndex(Class<S> type, int index) {
    return (S) byIndex.get(type, index);
  }

  public static @Nullable <S extends Slot> S atPosition(Class<S> type, int column, int row) {
    return forIndex(type, row * 9 + column);
  }

  public static <I extends Inventory> Class<? extends Slot> typeForInventory(Class<I> inv) {
    for (Map.Entry<Class<? extends Inventory>, Class<? extends Slot>> entry :
        byInventoryType.entrySet()) {
      if (entry.getKey().isAssignableFrom(inv)) {
        return entry.getValue();
      }
    }
    throw new IllegalStateException("Weird inventory type " + inv);
  }

  public static <I extends Inventory> Stream<? extends Slot> forInventory(Class<I> invType) {
    return all.stream().filter(typeForInventory(invType)::isInstance);
  }

  public static @Nullable <I extends Inventory> Slot forInventoryIndex(Class<I> inv, int index) {
    return forIndex(typeForInventory(inv), index);
  }

  public static @Nullable Slot forViewIndex(InventoryView view, int rawIndex) {
    final int cookedIndex = view.convertSlot(rawIndex);
    return forInventoryIndex(
        (rawIndex == cookedIndex ? view.getTopInventory() : view.getBottomInventory()).getClass(),
        cookedIndex);
  }

  private final String key;
  private final int index; // -1 = auto

  private Slot(Class<? extends Slot> type, String key, int index) {
    this.key = key;
    this.index = index;

    all.add(this);
    if (key != null) byKey.put(key, this);
    if (index >= 0) byIndex.put(type, index, this);
  }

  @Override
  public String toString() {
    return key != null ? getKey() : super.toString();
  }

  /** @return the name of this slot, as used by the /replaceitem command */
  public String getKey() {
    return key == null ? null : "slot." + key;
  }

  public boolean hasIndex() {
    return index >= 0;
  }

  /** @return a slot index that can be passed to {@link Inventory#getItem} et al. */
  public int getIndex() {
    if (!hasIndex()) throw new UnsupportedOperationException("Slot " + this + " has no index");
    return index;
  }

  public int getColumn() {
    return getIndex() % 9;
  }

  public int getRow() {
    return getIndex() / 9;
  }

  public int maxStackSize() {
    return 64;
  }

  public int maxStackSize(Material material) {
    return Math.min(maxStackSize(), material.getMaxStackSize());
  }

  public int maxStackSize(ItemStack item) {
    return maxStackSize(item.getType());
  }

  public int maxTransferrableIn(ItemStack source) {
    return Math.min(source.getAmount(), maxStackSize(source));
  }

  public int maxTransferrableIn(ItemStack source, Inventory inv) {
    final ItemStack dest = getItem(inv);
    if (Materials.isNothing(dest)) {
      return maxTransferrableIn(source);
    } else if (dest.isSimilar(source)) {
      return Math.min(source.getAmount(), Math.max(0, maxStackSize(dest) - dest.getAmount()));
    } else {
      return 0;
    }
  }

  public boolean isEquipment() {
    return false;
  }

  public EquipmentSlot toEquipmentSlot() {
    throw new UnsupportedOperationException("Slot " + this + " is not an equipment slot");
  }

  public Inventory getInventory(InventoryHolder holder) {
    return holder.getInventory();
  }

  protected static @Nullable ItemStack airToNull(ItemStack stack) {
    return stack == null || stack.getType() == Material.AIR ? null : stack;
  }

  public @Nullable ItemStack getItem(InventoryHolder holder) {
    return getItem(getInventory(holder));
  }

  public Optional<ItemStack> item(InventoryHolder holder) {
    return Optional.ofNullable(getItem(holder));
  }

  public void putItem(InventoryHolder holder, ItemStack stack) {
    putItem(getInventory(holder), stack);
  }

  /**
   * @return the item in this slot of the given holder's inventory, or null if the slot is empty.
   *     This will never return a stack of {@link Material#AIR}.
   */
  public @Nullable ItemStack getItem(Inventory inv) {
    return airToNull(inv.getItem(getIndex()));
  }

  public Optional<ItemStack> item(Inventory inv) {
    return Optional.ofNullable(getItem(inv));
  }

  public int amount(Inventory inv) {
    return Materials.amount(getItem(inv));
  }

  public boolean isEmpty(Inventory inv) {
    return amount(inv) == 0;
  }

  /** Put the given stack in this slot of the given holder's inventory. */
  public void putItem(Inventory inv, ItemStack stack) {
    inv.setItem(getIndex(), airToNull(stack));
  }

  protected PlayerInventory asPlayerInventory(Inventory inv) {
    if (inv instanceof PlayerInventory plInv) return plInv;
    throw new IllegalArgumentException("Slot " + this + " is player-only inventory slot");
  }

  protected org.bukkit.entity.Player asPlayer(InventoryHolder holder) {
    if (holder instanceof org.bukkit.entity.Player player) return player;
    throw new IllegalArgumentException("Slot " + this + " is player-only inventory slot");
  }

  public static class Container extends Slot {
    static void init() {
      for (int i = 0; i < 54; i++) {
        new Container("container." + i, i);
      }
    }

    public static @Nullable Container forIndex(int index) {
      return forIndex(Container.class, index);
    }

    Container(String key, int index) {
      super(Container.class, key, index);
    }
  }

  public abstract static class Player extends Slot {
    static void init() {
      Storage.init();
      Equipment.init();
      Cursor.init();
    }

    public static Stream<Player> player() {
      return Stream.concat(
          Stream.concat(Storage.storage(), Equipment.equipment()), Stream.of(Cursor.cursor()));
    }

    Player(String key, int index) {
      super(Player.class, key, index);
    }

    public static @Nullable Player forIndex(int index) {
      return forIndex(Player.class, index);
    }

    @Override
    public @Nullable ItemStack getItem(Inventory generic) {
      var inv = asPlayerInventory(generic);
      return isEquipment() ? airToNull(inv.getItem(toEquipmentSlot())) : super.getItem(inv);
    }

    @Override
    public void putItem(Inventory generic, ItemStack stack) {
      var inv = asPlayerInventory(generic);
      if (isEquipment()) {
        inv.setItem(toEquipmentSlot(), stack);
      } else {
        super.putItem(inv, stack);
      }
    }
  }

  public static class Storage extends Player {
    static void init() {
      Hotbar.init();
      Pockets.init();
    }

    public static Stream<? extends Storage> storage() {
      return Stream.concat(Hotbar.hotbar(), Pockets.pockets());
    }

    Storage(String key, int index) {
      super(key, index);
    }
  }

  public static class Hotbar extends Storage {
    static void init() {
      hotbar = new Hotbar[9];
      for (int i = 0; i < 9; i++) {
        hotbar[i] = new Hotbar("hotbar." + i, i);
      }
    }

    private static Hotbar[] hotbar;

    public static Stream<? extends Hotbar> hotbar() {
      return Stream.of(hotbar);
    }

    public static Hotbar forPosition(int pos) {
      return (Hotbar) forIndex(pos);
    }

    protected Hotbar(String key, int index) {
      super(key, index);
    }
  }

  public static class Pockets extends Storage {
    static void init() {
      pockets = new Pockets[27];
      for (int i = 0; i < 27; i++) {
        pockets[i] = new Pockets("inventory." + i, 9 + i);
      }
      MainHand.init();
    }

    private static Pockets[] pockets;

    public static Stream<? extends Pockets> pockets() {
      return Stream.of(pockets);
    }

    protected Pockets(String key, int index) {
      super(key, index);
    }
  }

  public abstract static class Equipment extends Player {
    static void init() {
      OffHand.init();
      Armor.init();
    }

    public static Stream<? extends Equipment> equipment() {
      return Stream.concat(Stream.of(OffHand.offHand()), Armor.armor());
    }

    private final EquipmentSlot equipmentSlot;

    Equipment(String key, int index, EquipmentSlot equipmentSlot) {
      super(key, index);
      this.equipmentSlot = equipmentSlot;
    }

    @Override
    public int maxStackSize() {
      return 1;
    }

    @Override
    public boolean isEquipment() {
      return true;
    }

    @Override
    public EquipmentSlot toEquipmentSlot() {
      return equipmentSlot;
    }
  }

  public static class MainHand extends Hotbar {
    static void init() {
      mainHand = new MainHand();
    }

    private static MainHand mainHand;

    public static MainHand mainHand() {
      return mainHand;
    }

    MainHand() {
      super("weapon.mainhand", -1);
    }

    @Override
    public boolean isEquipment() {
      return true;
    }

    @Override
    public EquipmentSlot toEquipmentSlot() {
      return EquipmentSlot.HAND;
    }
  }

  public static class OffHand extends Equipment {
    static void init() {
      if (Platform.isLegacy()) return;
      offHand = new OffHand();
    }

    private static OffHand offHand;

    public static OffHand offHand() {
      if (Platform.isLegacy())
        throw new UnsupportedOperationException("OffHand is not supported on legacy platform");
      return offHand;
    }

    protected OffHand() {
      super("weapon.offhand", 40, EquipmentSlot.valueOf("OFF_HAND"));
    }
  }

  public static class Armor extends Equipment {
    static void init() {
      new Armor("armor.feet", EquipmentSlot.FEET, ArmorType.BOOTS);
      new Armor("armor.legs", EquipmentSlot.LEGS, ArmorType.LEGGINGS);
      new Armor("armor.chest", EquipmentSlot.CHEST, ArmorType.CHESTPLATE);
      new Armor("armor.head", EquipmentSlot.HEAD, ArmorType.HELMET);
    }

    private static final Map<ArmorType, Armor> byArmorType = new EnumMap<>(ArmorType.class);

    public static Stream<? extends Armor> armor() {
      return byArmorType.values().stream();
    }

    private final ArmorType armorType;

    Armor(String key, EquipmentSlot equipmentSlot, ArmorType armorType) {
      super(key, armorType.inventorySlot(), equipmentSlot);
      this.armorType = armorType;
      byArmorType.put(armorType, this);
    }

    public ArmorType getArmorType() {
      return armorType;
    }

    public static Armor forType(ArmorType armorType) {
      return byArmorType.get(armorType);
    }
  }

  public static class EnderChest extends Slot {
    static void init() {
      for (int i = 0; i < 27; i++) {
        new EnderChest("enderchest." + i, i);
      }
    }

    EnderChest(String key, int index) {
      super(EnderChest.class, key, index);
    }

    @Override
    public Inventory getInventory(InventoryHolder holder) {
      return asPlayer(holder).getEnderChest();
    }
  }

  public static class Cursor extends Player {
    static void init() {
      cursor = new Cursor();
    }

    private static Cursor cursor;

    public static Cursor cursor() {
      return cursor;
    }

    Cursor() {
      super(null, -1);
    }

    @Override
    public String toString() {
      return "cursor";
    }

    @Override
    public @Nullable ItemStack getItem(InventoryHolder holder) {
      return airToNull(asPlayer(holder).getItemOnCursor());
    }

    @Override
    public void putItem(InventoryHolder holder, @Nullable ItemStack stack) {
      asPlayer(holder).setItemOnCursor(stack);
    }

    @Override
    public @Nullable ItemStack getItem(Inventory inv) {
      return getItem(asPlayerInventory(inv).getHolder());
    }

    @Override
    public void putItem(Inventory inv, ItemStack stack) {
      putItem(asPlayerInventory(inv).getHolder(), stack);
    }
  }
}

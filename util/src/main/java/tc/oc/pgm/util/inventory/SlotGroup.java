package tc.oc.pgm.util.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class SlotGroup implements Iterable<Slot.Player> {
  private static final Map<String, SlotGroup> GROUPS_BY_KEY = new HashMap<>();

  public static final SlotGroup ALL = register("all", Slot.Player.player());

  // Everything except armor
  public static final SlotGroup CARRYING = register(
      "carrying",
      Stream.concat(
          Stream.concat(Slot.Storage.storage(), Slot.OffHand.offHand().stream()),
          Stream.concat(Stream.of(Slot.Cursor.cursor()), Slot.Crafting.crafting())));

  public static final SlotGroup STORAGE = register("storage", Slot.Storage.storage());
  public static final SlotGroup HOTBAR = register("hotbar", Slot.Hotbar.hotbar());
  public static final SlotGroup POCKETS = register("pockets", Slot.Pockets.pockets());

  public static final SlotGroup EQUIPMENT = register("equipment", Slot.Equipment.equipment());
  public static final SlotGroup OFFHAND = register("offhand", Slot.OffHand.offHand().stream());
  public static final SlotGroup ARMOR = register("armor", Slot.Armor.armor());

  public static final SlotGroup HANDS = register("hands", Slot.Equipment.hands());
  public static final SlotGroup MAINHAND =
      register("mainhand", Stream.of(Slot.MainHand.mainHand()));

  public static final SlotGroup CURSOR = register("cursor", Stream.of(Slot.Cursor.cursor()));
  public static final SlotGroup CRAFTING = register("crafting", Slot.Crafting.crafting());

  static {
    GROUPS_BY_KEY.put("hand", MAINHAND);
  }

  private static SlotGroup register(String name, Stream<? extends Slot.Player> slots) {
    var group = new SlotGroup(slots.map(s -> (Slot.Player) s).toList());
    GROUPS_BY_KEY.put(name, group);
    return group;
  }

  public static @Nullable SlotGroup forKey(String key) {
    return GROUPS_BY_KEY.get(StringUtils.normalize(key));
  }

  private final List<Slot.Player> slots;

  private SlotGroup(List<Slot.Player> slots) {
    this.slots = slots;
  }

  public static Builder builder(Node node) {
    return new Builder(node);
  }

  @Override
  public @NonNull Iterator<Slot.Player> iterator() {
    return slots.iterator();
  }

  public Stream<Slot.Player> stream() {
    return slots.stream();
  }

  public Stream<ItemStack> stream(PlayerInventory inv) {
    return slots.stream().map(s -> s.getItem(inv)).filter(Objects::nonNull);
  }

  public void forEach(PlayerInventory inv, BiConsumer<Slot.Player, ItemStack> consumer) {
    forEach(s -> {
      var item = s.getItem(inv);
      if (item != null) consumer.accept(s, s.getItem(inv));
    });
  }

  public boolean containsAny(SlotGroup other) {
    return !Collections.disjoint(slots, other.slots);
  }

  public static class Builder {
    private final Node node;
    private final SequencedSet<SlotGroup> groups = new LinkedHashSet<>();
    private final SequencedSet<Slot.Player> slots = new LinkedHashSet<>();
    private boolean groupsOnly = true;

    public Builder(Node node) {
      this.node = node;
    }

    public void addGroup(String text, SlotGroup group) throws InvalidXMLException {
      if (!groups.add(group))
        throw new InvalidXMLException("Duplicate slot group '" + text + "'", node);
      if (group.slots.isEmpty()) return; // Can happen with offhand on legacy, just no-op
      if (!slots.addAll(group.slots))
        throw new InvalidXMLException(
            "Slot group '" + text + "' is fully covered by earlier slots", node);
    }

    public void addSlot(String text, Slot.Player slot) throws InvalidXMLException {
      groupsOnly = false;
      if (!slots.add(slot))
        throw new InvalidXMLException(
            "Slot '" + text + "' is already defined by an earlier group", node);
    }

    public SlotGroup build() throws InvalidXMLException {
      // Easy solve, single-group
      if (groups.size() == 1 && groupsOnly) return groups.getFirst();

      // Prevent bugs that would happen from iterating twice over the mainhand slot
      if (slots.contains(Slot.MainHand.mainHand())
          && Slot.Hotbar.hotbar().anyMatch(slots::contains)) {
        throw new InvalidXMLException(
            "Main hand and hotbar slots cannot both be present as they overlap", node);
      }
      var wouldBeSlots = List.copyOf(slots);

      // In case someone defines a known mapping, reuse the known object, eg:
      // "storage,equipment,cursor" is just "all", or "hotbar,pockets" is just "storage"
      return GROUPS_BY_KEY.values().stream()
          .filter(group -> group.slots.equals(wouldBeSlots))
          .findAny()
          .orElseGet(() -> new SlotGroup(wouldBeSlots));
    }
  }
}

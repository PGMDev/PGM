package tc.oc.pgm.loot;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.InventoryQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.filters.query.EntityQuery;
import tc.oc.pgm.itemmeta.ItemModifyMatchModule;
import tc.oc.pgm.util.Pair;
import tc.oc.pgm.util.collection.InstantMap;
import tc.oc.pgm.util.inventory.Slot;

@ListenerScope(value = MatchScope.LOADED)
public class LootableMatchModule implements MatchModule, Listener {

  private final Logger logger;
  private final Match match;
  private final List<FillerDefinition> fillers;
  private final List<Cache> caches;

  private final InstantMap<Pair<Fillable, FillerDefinition>> filledAt;
  private final @Nullable ItemModifyMatchModule immm;

  public LootableMatchModule(
      Logger logger, Match match, List<FillerDefinition> fillers, List<Cache> caches) {
    this.logger = logger;
    this.fillers = fillers;
    this.match = match;
    this.caches = caches;
    this.filledAt = new InstantMap<>(match.getClock());
    this.immm = match.getModule(ItemModifyMatchModule.class);

    final FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    fillers.forEach(filler -> fmm.onRise(Match.class, filler.getRefillTrigger(), m -> this.filledAt
        .keySet()
        .removeIf(f -> filler.equals(f.getRight()))));
  }

  /**
   * Return a predicate that applies a Filter to the given InventoryHolder, or null if the
   * InventoryHolder is not something that we should be filling.
   */
  private static @Nullable Predicate<Filter> filterPredicate(InventoryHolder holder) {
    if (holder instanceof DoubleChest doubleChest) {
      return filter -> !filter
              .query(new BlockQuery((Chest) doubleChest.getLeftSide()))
              .isDenied()
          || !filter.query(new BlockQuery((Chest) doubleChest.getRightSide())).isDenied();
    } else if (holder instanceof BlockState) {
      return filter -> !filter.query(new BlockQuery((BlockState) holder)).isDenied();
    } else if (holder instanceof Entity && !(holder instanceof Player)) {
      return filter -> !filter.query(new EntityQuery((Entity) holder)).isDenied();
    } else {
      // This happens with crafting inventories, and possibly other transient inventory types
      // Pretty sure we never want to fill an inventory held by the player, or one we don't know
      // about
      return null;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event) {
    final MatchPlayer opener = this.match.getParticipant(event.getPlayer());
    if (opener == null) return;

    final Inventory inventory = event.getInventory();
    final Predicate<Filter> filterPredicate = filterPredicate(inventory.getHolder());
    if (filterPredicate == null) return;

    logger.fine(() ->
        opener.getNameLegacy() + " opened a " + inventory.getHolder().getClass().getSimpleName());

    // Find all Fillers that apply to the holder of the opened inventory
    final List<FillerDefinition> fillers = this.fillers.stream()
        .filter(filler -> filterPredicate.test(filler.getFilter()))
        .collect(Collectors.toList());
    if (fillers.isEmpty()) return;

    logger.fine(() -> "Found fillers "
        + fillers.stream().map(FillerDefinition::toString).collect(Collectors.joining(", ")));

    // Find all Caches that the opened inventory is part of
    final List<Fillable> fillables = new ArrayList<>();
    for (Cache cache : caches) {
      if (filterPredicate.test(cache.jointFilter())) {
        fillables.add(new FillableCache(cache));
      }
    }
    // If the inventory is not in any Cache, just fill it directly
    if (fillables.isEmpty()) {
      fillables.add(new FillableInventory(inventory));
    }

    fillables.forEach(fillable -> fillable.fill(opener, fillers));
  }

  abstract class Fillable {

    abstract Stream<Inventory> inventories();

    void fill(MatchPlayer opener, List<FillerDefinition> fillers) {
      // Build a short list of Fillers that are NOT cooling down from a previous fill
      final List<FillerDefinition> coolFillers = fillers.stream()
          .filter(filler ->
              filledAt.putUnlessNewer(new Pair<>(this, filler), filler.getRefillInterval()) == null)
          .toList();

      // Find all the Inventories for this Fillable, and build a map of Fillers to the subset
      // of Inventories that they are allowed to fill, based on the filter of each Filler.
      // Note how duplicate inventories are skipped.
      final SetMultimap<FillerDefinition, Inventory> fillerInventories = HashMultimap.create();
      inventories().distinct().forEach(inventory -> {
        final Predicate<Filter> passes = filterPredicate(inventory.getHolder());
        if (passes == null) return;
        for (FillerDefinition filler : coolFillers) {
          if (passes.test(filler.getFilter())) {
            fillerInventories.put(filler, inventory);
          }
        }
      });

      fillerInventories.asMap().forEach((filler, inventories) -> {
        if (filler.cleanBeforeRefill()) {
          inventories().forEach(Inventory::clear);
        }
      });

      final Random random = new Random();

      fillerInventories.asMap().forEach((filler, inventories) -> {
        // For each Filler, build a mutable list of slots that it can fill.
        // (27 is the standard size for single chests)
        final List<InventorySlot> slots = new ArrayList<>(inventories.size() * 27);
        for (Inventory inv : inventories) {
          for (int index = 0; index < inv.getSize(); index++) {
            if (inv.getItem(index) == null) {
              slots.add(InventorySlot.fromInventoryIndex(inv, index));
            }
          }
        }

        filler
            .getLoot()
            .elements(opener)
            .limit(slots.size())
            .peek(i -> {
              if (immm != null) immm.applyRules(i);
            })
            .forEachOrdered(i -> slots.remove(random.nextInt(slots.size())).putItem(i));
      });
    }
  }

  private class FillableInventory extends Fillable {
    final Inventory inventory;

    FillableInventory(Inventory inventory) {
      this.inventory = inventory;
    }

    @Override
    public int hashCode() {
      return inventory.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof FillableInventory
          && inventory.equals(((FillableInventory) obj).inventory);
    }

    @Override
    Stream<Inventory> inventories() {
      return Stream.of(inventory);
    }
  }

  private class FillableCache extends Fillable {
    final Cache cache;

    private FillableCache(Cache cache) {
      this.cache = cache;
    }

    @Override
    public int hashCode() {
      return cache.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof FillableCache && cache.equals(((FillableCache) obj).cache);
    }

    @Override
    Stream<Inventory> inventories() {
      return cache
          .region()
          .getChunkPositions()
          .map(cp -> cp.getChunk(match.getWorld()))
          .<InventoryQuery>flatMap(ch -> Stream.concat(
              Stream.of(ch.getTileEntities()).map(BlockQuery::new),
              Stream.of(ch.getEntities())
                  .filter(e -> !(e instanceof Player))
                  .map(EntityQuery::new)))
          .filter(q -> cache.jointFilter().query(q).isAllowed())
          .map(InventoryQuery::getInventory)
          .filter(Objects::nonNull);
    }
  }

  /** A wrapper of a slot that belongs to a specified {@link Inventory} */
  private static class InventorySlot {

    private final Inventory inventory;
    private final Slot slot;

    private InventorySlot(Inventory inventory, Slot slot) {
      assertNotNull(inventory, "inventory");
      assertNotNull(slot, "slot");
      this.inventory = inventory;
      this.slot = slot;
    }

    static InventorySlot fromInventoryIndex(Inventory inventory, int index) {
      final Slot slot = Slot.forInventoryIndex(inventory.getClass(), index);
      if (slot == null)
        throw new IllegalArgumentException(
            "Could not determine slot at index " + index + " in inventory " + inventory);
      return new InventorySlot(inventory, slot);
    }

    private void putItem(ItemStack item) {
      this.slot.putItem(inventory, item);
    }
  }
}

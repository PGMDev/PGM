package tc.oc.pgm.loot;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;
import tc.oc.pgm.filters.query.BlockQuery;

@ListenerScope(MatchScope.RUNNING)
public class LootMatchModule implements MatchModule, Listener {

  // Things with inventories that can be kept inside.
  // Storage entities (storage and hopper minecarts) currently don't work.
  // Ender chests are always disabled.
  private static final Material[] CONTAINERS = {
    Material.CHEST,
    Material.TRAPPED_CHEST,
    Material.DISPENSER,
    Material.DROPPER,
    Material.HOPPER,
    Material.BREWING_STAND,
    Material.FURNACE,
    Material.BEACON,
    Material.STORAGE_MINECART,
    Material.HOPPER_MINECART
  };
  private final Match match;
  private final List<LootableDefinition> definitions;
  private final List<FillableCache> fillableCaches;
  private final List<LootCountdown> lootCountdowns;
  private final CountdownContext countdownContext;

  public LootMatchModule(Match match, List<LootableDefinition> definitions) {
    this.match = match;
    this.definitions = definitions;
    this.fillableCaches = new ArrayList<>(this.definitions.size());
    this.lootCountdowns = new ArrayList<>(this.definitions.size());
    this.countdownContext = new CountdownContext(match, match.getLogger());
  }

  @Override
  public void load() throws ModuleLoadException {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    for (LootableDefinition definition : this.definitions) {
      LootCountdown countdown = new LootCountdown(match, this, definition);
      this.lootCountdowns.add(countdown);
      // dynamic filter refill-trigger (example in ObjectiveModesMatchModule)

      // looking at the code it seems like if a chest is opened that's in the defined region and
      // passes the filter the inventory of it will be cached then when that chest is set to refill
      // it will populate it using what items were in there from the start rather than rolling a
      // loot table
      if (definition.getRefillTrigger() != null) {
        fmm.onRise(
            MatchPlayer.class,
            definition.getRefillTrigger(),
            listener -> {
              // TODO figure this out
            });
      }
    }
  }

  @Override
  public void disable() {
    for (LootCountdown countdown : this.getAllCountdowns()) {
      this.countdownContext.cancel(countdown);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event) {
    Block clickedMaterial = event.getPlayer().getTargetBlock((Set<Material>) null, 5);
    if (ArrayUtils.contains(CONTAINERS, clickedMaterial.getType())) {
      MatchPlayer matchPlayer = match.getPlayer(event.getPlayer());
      Inventory containerInventory = event.getInventory();
      for (LootableDefinition definition : definitions) {
        for (LootCountdown countdown : getAllCountdowns()) {
          if (countdown.getLootableDefinition().equals(definition)) {
            BlockQuery query = new BlockQuery(event, clickedMaterial);
            // filter defined in <fill>
            if (definition.filter.query(query).isAllowed()) {
              if (definition.refillClear) {
                containerInventory.clear();
              }
              // add items that will always be in loot
              for (Loot loot : definition.lootableItems) {
                containerInventory.addItem(loot.getStack());
              }
              // add maybe items
              for (Maybe maybe : definition.maybeLootables) {
                if (maybe.getFilter().query(matchPlayer).isAllowed()) {
                  for (Loot loot : maybe.getMaybeItems()) {
                    containerInventory.addItem(loot.getStack());
                  }
                }
              }
              // add any items
              for (Any any : definition.anyLootables) {
                Random rand = match.getRandom();
                if (!any.getAnyItems().isEmpty()) {
                  List<Loot> anyItems = any.getAnyItems();
                  // TODO make count range work
                  for (int i = 0; i < any.getCount(); ) {
                    Loot chosenItem = anyItems.get(rand.nextInt(anyItems.size()));
                    containerInventory.addItem(chosenItem.getStack());
                    if (any.isUnique()) {
                      anyItems.remove(chosenItem);
                    }
                    i++;
                  }
                }
                if (!any.getOptions().isEmpty()) {
                  List<Loot> anyItems = any.getAnyItems();
                  // TODO implement weight probability
                  for (int i = 0; i < any.getCount(); ) {
                    Option chosenOption =
                        any.getOptions().get(rand.nextInt(any.getOptions().size()));
                    if (chosenOption.getFilter().query(matchPlayer).isAllowed()) {
                      containerInventory.addItem(chosenOption.getItem().getStack());
                      if (any.isUnique()) {
                        anyItems.remove(chosenOption.getItem());
                      }
                      // do we still count the option if it is ineligible by the filter?
                      i++;
                    }
                  }
                }
              }
            }
          }
        }
        // Find all Caches that the opened inventory is part of
        if (definition.getCache() != null) {
          // if chest is in region and passes filter, fix this
          if (definition.getCache().getFilter().query(matchPlayer).isAllowed()) {
            FillableCache fillableCache =
                new FillableCache(containerInventory, definition.getId(), definition.getCache());
            if (fillableCaches.contains(fillableCache)) {
              containerInventory = fillableCache.getInventory();
            } else {
              this.fillableCaches.add(
                  new FillableCache(containerInventory, definition.getId(), definition.getCache()));
            }
          }
        }
        // start refill interval
        for (LootCountdown countdown : lootCountdowns) {
          if (countdown.getLootableDefinition().equals(definition)) {
            this.countdownContext.start(countdown, definition.refillInterval);
          }
        }
      }
    }
  }

  public CountdownContext getCountdown() {
    return this.countdownContext;
  }

  public List<LootCountdown> getAllCountdowns() {
    return new ImmutableList.Builder<LootCountdown>()
        .addAll(this.countdownContext.getAll(LootCountdown.class))
        .build();
  }
}

package tc.oc.pgm.loot;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
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
  private final List<LootCountdown> lootCountdowns;
  private final CountdownContext countdownContext;

  public LootMatchModule(Match match, List<LootableDefinition> definitions) {
    this.match = match;
    this.definitions = definitions;
    this.lootCountdowns = new ArrayList<>(this.definitions.size());
    this.countdownContext = new CountdownContext(match, match.getLogger());
  }

  @Override
  public void load() throws ModuleLoadException {
    for (LootableDefinition definition : this.definitions) {
      LootCountdown countdown = new LootCountdown(match, this, definition);
      this.lootCountdowns.add(countdown);
      // dynamic filter refill-trigger (example in ObjectiveModesMatchModule)
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
            // TODO add dynamic filter refill-trigger
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

  public List<LootCountdown> getInctiveCountdowns() {
    List<LootCountdown> inactiveCountdowns =
        new ArrayList<>(
            Collections2.filter(
                this.getAllCountdowns(),
                new Predicate<LootCountdown>() {
                  @Override
                  public boolean apply(@Nullable LootCountdown countdown) {
                    return LootMatchModule.this.getCountdown().getTimeLeft(countdown).getSeconds()
                        < 0;
                  }
                }));
    Collections.sort(inactiveCountdowns);

    return inactiveCountdowns;
  }
}

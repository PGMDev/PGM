package tc.oc.pgm.wool;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.wool.MonumentWoolFactory.WOOL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.teams.Team;

@ListenerScope(MatchScope.RUNNING)
public class WoolMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Multimap<Team, MonumentWool> wools;

  // Map of containers to a flag indicating whether they contained objective wool when the match
  // started.
  // For this to work, containers have to be checked for wool before their contents can be changed.
  // To ensure this,
  // containers are registered in this map the first time they are opened or accessed by a hopper or
  // dispenser.
  private final Map<Inventory, Boolean> chests = new HashMap<>();

  // Containers that did contain wool when the match started have an entry in this map representing
  // the exact
  // layout of the wools in the inventory. This is used to refill the container with wools.
  private final Map<Inventory, Map<Integer, ItemStack>> woolChests = new HashMap<>();

  private static final int REFILL_INTERVAL = 30; // seconds

  public WoolMatchModule(Match match, Multimap<Team, MonumentWool> wools) {
    this.match = match;
    this.wools = wools;
  }

  @Override
  public void enable() {
    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleWithFixedDelay(
            this::refillOneWoolPerContainer, 0, REFILL_INTERVAL, TimeUnit.SECONDS);
  }

  public Multimap<Team, MonumentWool> getWools() {
    return wools;
  }

  private boolean isObjectiveWool(ItemStack stack) {
    if (WOOL.matches(stack.getType())) {
      for (MonumentWool wool : this.wools.values()) {
        if (wool.getDefinition().isObjectiveWool(stack)) return true;
      }
    }
    return false;
  }

  private boolean containsObjectiveWool(Inventory inventory) {
    for (MonumentWool wool : this.wools.values()) {
      if (wool.getDefinition().isHolding(inventory)) return true;
    }
    return false;
  }

  private void registerContainer(Inventory inv) {
    // When a chest (or other block inventory) is accessed, check if it's a wool chest
    Boolean isWoolChest = this.chests.get(inv);
    if (isWoolChest == null) {
      // If we haven't seen this chest yet, check it for wool
      isWoolChest = this.containsObjectiveWool(inv);
      this.chests.put(inv, isWoolChest);

      if (isWoolChest) {
        // If it is a wool chest, take a snapshot of the wools
        Map<Integer, ItemStack> contents = new HashMap<>();
        this.woolChests.put(inv, contents);
        for (int slot = 0; slot < inv.getSize(); ++slot) {
          ItemStack stack = inv.getItem(slot);
          if (stack != null && this.isObjectiveWool(stack)) {
            contents.put(slot, stack.clone());
          }
        }
      }
    }
  }

  private void refillOneWoolPerContainer() {
    if (!PGM.get().getConfiguration().shouldRefillWool()) return;

    for (Entry<Inventory, Map<Integer, ItemStack>> chest : this.woolChests.entrySet()) {
      Inventory inv = chest.getKey();
      for (Entry<Integer, ItemStack> slotEntry : chest.getValue().entrySet()) {
        int slot = slotEntry.getKey();
        ItemStack wool = slotEntry.getValue();
        ItemStack stack = inv.getItem(slotEntry.getKey());

        if (stack == null) {
          stack = wool.clone();
          stack.setAmount(1);
          inv.setItem(slot, stack);
          break;
        } else if (stack.isSimilar(wool) && stack.getAmount() < wool.getAmount()) {
          stack.setAmount(stack.getAmount() + 1);
          inv.setItem(slot, stack);
          break;
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event) {
    // Register container blocks when they are opened
    this.registerContainer(event.getInventory());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemTransfer(InventoryMoveItemEvent event) {
    // When a hopper or dispenser transfers an item, register both blocks involved
    this.registerContainer(event.getSource());
    this.registerContainer(event.getDestination());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onContainerPlace(BlockPlaceEvent event) {
    // Blacklist any placed container blocks
    if (event.getBlock().getState() instanceof InventoryHolder) {
      this.chests.put(((InventoryHolder) event.getBlock().getState()).getInventory(), false);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void placementCheck(final BlockTransformEvent event) {
    if (this.match.getWorld() != event.getWorld()) return;

    Entry<Team, MonumentWool> woolEntry =
        this.findMonumentWool(event.getNewState().getBlock());
    if (woolEntry == null) return;

    MonumentWool wool = woolEntry.getValue();

    if (event.getNewState().getType() == Material.AIR) { // block is being destroyed
      if (wool.getDefinition().isObjectiveWool(event.getOldState())) {
        event.setCancelled(true);
      }
      return;
    }

    // default to cancelled; only uncancel if player is placing the correct color wool (see below)
    event.setCancelled(true);

    ParticipantState player = ParticipantBlockTransformEvent.getPlayerState(event);
    if (player != null) { // wool can only be placed by a player
      Component woolName = wool.getComponentName();
      if (!wool.getDefinition().isObjectiveWool(event.getNewState())) {
        player.sendWarning(translatable("wool.wrongWool", woolName));
      } else if (wool.getOwner() != player.getParty()) {
        player.sendWarning(translatable("wool.wrongTeam", wool.getOwner().getName(), woolName));
      } else {
        event.setCancelled(false);
        wool.markPlaced();
        this.match.callEvent(new GoalStatusChangeEvent(match, wool, wool.getOwner()));
        this.match.callEvent(new PlayerWoolPlaceEvent(player, wool, event.getNewState()));
        this.match.callEvent(new GoalCompleteEvent(
            this.match,
            wool,
            wool.getOwner(),
            true,
            ImmutableList.of(new Contribution(player, 1))));
      }
    }
  }

  @EventHandler
  public void handleWoolCrafting(PrepareItemCraftEvent event) {
    Recipe recipe = event.getRecipe();
    if (recipe == null) return;

    ItemStack result = recipe.getResult();
    InventoryHolder holder = event.getInventory().getHolder();

    if (holder instanceof Player) {
      MatchPlayer playerHolder = this.match.getPlayer((Player) holder);

      if (playerHolder != null && result != null && WOOL.matches(result.getType())) {
        for (MonumentWool wool : this.wools.values()) {
          if (wool.getDefinition().isObjectiveWool(result)) {
            if (!wool.getDefinition().isCraftable()) {
              playerHolder.sendWarning(
                  translatable("wool.craftingDisabled", wool.getComponentName()));
              event.getInventory().setResult(null);
            }
          }
        }
      }
    }
  }

  private Entry<Team, MonumentWool> findMonumentWool(Block block) {
    for (Entry<Team, MonumentWool> woolEntry : this.wools.entries()) {
      if (woolEntry.getValue().getDefinition().getPlacementRegion().contains(block)) {
        return woolEntry;
      }
    }
    return null;
  }
}

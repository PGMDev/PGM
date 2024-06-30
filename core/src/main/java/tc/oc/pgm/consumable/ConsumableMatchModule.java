package tc.oc.pgm.consumable;

import com.google.common.collect.ImmutableMap;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.inventory.InventoryUtils;

public class ConsumableMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ImmutableMap<String, ConsumableDefinition> consumables;

  public ConsumableMatchModule(
      Match match, ImmutableMap<String, ConsumableDefinition> consumables) {
    this.match = match;
    this.consumables = consumables;
  }

  private @Nullable ConsumableDefinition getConsumableDefinition(ItemStack item) {
    return consumables.get(ItemTags.CONSUMABLE.get(item));
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  private void onItemConsume(PlayerItemConsumeEvent event) {
    ConsumableDefinition consumable = getConsumableDefinition(event.getItem());
    if (consumable == null || consumable.getCause() != ConsumeCause.EAT) return;

    runConsumable(event, consumable);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  private void onClick(PlayerInteractEvent event) {
    ConsumableDefinition consumable = getConsumableDefinition(event.getItem());
    if (consumable == null) return;

    var action = event.getAction();
    if (!switch (consumable.getCause()) {
      case LEFT_CLICK -> action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR;
      case RIGHT_CLICK -> action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
      case CLICK -> action == Action.LEFT_CLICK_BLOCK
          || action == Action.LEFT_CLICK_AIR
          || action == Action.RIGHT_CLICK_BLOCK
          || action == Action.RIGHT_CLICK_AIR;
      default -> false;
    }) return;

    runConsumable(event, consumable);
  }

  public <T extends PlayerEvent & Cancellable> void runConsumable(
      T event, ConsumableDefinition consumable) {
    MatchPlayer matchPlayer = match.getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(matchPlayer)) return;

    if (consumable.getOverride()) {
      event.setCancelled(true);
    }

    boolean naturallyConsumes =
        event instanceof PlayerItemConsumeEvent && !consumable.getOverride();
    if (consumable.getConsume() && !naturallyConsumes) {
      InventoryUtils.consumeItem(event);
    }

    consumable.getAction().trigger(matchPlayer);
  }
}

package tc.oc.pgm.consumable;

import com.google.common.collect.ImmutableMap;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
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

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  private void onItemConsume(PlayerItemConsumeEvent event) {
    runConsumable(event, event.getItem(), ConsumeCause.EAT);
  }

  @EventHandler(priority = EventPriority.HIGH)
  private void onClick(PlayerInteractEvent event) {
    // It's been canceled
    if (event.useItemInHand() == Event.Result.DENY) return;

    runConsumable(
        event,
        event.getItem(),
        switch (event.getAction()) {
          case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> ConsumeCause.LEFT_CLICK;
          case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> ConsumeCause.RIGHT_CLICK;
          default -> null;
        });
  }

  private <T extends PlayerEvent & Cancellable> void runConsumable(
      T event, ItemStack item, ConsumeCause cause) {
    if (item == null || cause == null) return;
    MatchPlayer pl = match.getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(pl)) return;
    var ids = ItemTags.CONSUMABLE.get(item);
    if (ids == null) return;

    for (String id : ids) {
      var consumable = consumables.get(id);
      if (consumable == null || !consumable.getCause().triggersOn(cause)) continue;
      if (consumable.getOverride()) event.setCancelled(true);
      if (consumable.getConsume()) InventoryUtils.consumeItem(event, pl.getBukkit());
      consumable.getAction().trigger(pl);
      return;
    }
  }
}

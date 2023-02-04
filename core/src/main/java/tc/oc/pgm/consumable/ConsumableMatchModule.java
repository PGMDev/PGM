package tc.oc.pgm.consumable;

import com.google.common.collect.ImmutableSet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.kits.tag.ItemTags;

public class ConsumableMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ImmutableSet<ConsumableDefinition> consumableDefinitions;

  public ConsumableMatchModule(
      Match match, ImmutableSet<ConsumableDefinition> consumableDefinitions) {
    this.match = match;
    this.consumableDefinitions = consumableDefinitions;
  }

  @EventHandler
  private void onItemConsume(PlayerItemConsumeEvent event) {
    ConsumableDefinition consumableDefinition = getConsumableDefinition(event.getItem());
    if (consumableDefinition == null) return;
    if (consumableDefinition.getCause() != ConsumeCause.EAT) return;

    Player player = event.getPlayer();
    MatchPlayer matchPlayer = match.getPlayer(player);
    ParticipantState playerState = match.getParticipantState(player);
    if (playerState == null) return;

    if (consumableDefinition.getPreventDefault()) {
      ItemStack itemInHand = player.getItemInHand();
      if (itemInHand.getAmount() > 1) {
        itemInHand.setAmount(itemInHand.getAmount() - 1);
      } else {
        player.setItemInHand(null);
      }
      event.setCancelled(true);
    }

    consumableDefinition.getAction().trigger(matchPlayer);
  }

  private @Nullable ConsumableDefinition getConsumableDefinition(ItemStack item) {
    String consumableId = ItemTags.CONSUMABLE.get(item);
    if (consumableId != null) {
      for (ConsumableDefinition consumableDefinition : consumableDefinitions) {
        if (consumableDefinition.getId().equals(consumableId)) {
          return consumableDefinition;
        }
      }
    }

    return null;
  }
}

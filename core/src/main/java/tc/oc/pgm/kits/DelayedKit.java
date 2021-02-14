package tc.oc.pgm.kits;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public abstract class DelayedKit extends AbstractKit {

  public abstract void applyDelayed(MatchPlayer player, boolean force);

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    Party party = player.getParty();
    player
        .getMatch()
        .getExecutor(MatchScope.RUNNING)
        .schedule(
            () -> {
              if (player.isAlive() && player.getParty().equals(party)) {
                applyDelayed(player, force);
              }
            },
            50,
            TimeUnit.MILLISECONDS);
  }
}

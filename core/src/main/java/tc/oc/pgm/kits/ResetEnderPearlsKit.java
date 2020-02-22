package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

/** Disowns any Ender Pearls the player has thrown */
public class ResetEnderPearlsKit extends AbstractKit {
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    Player bukkitPlayer = player.getBukkit();
    for (EnderPearl pearl : bukkitPlayer.getWorld().getEntitiesByClass(EnderPearl.class)) {
      if (pearl.getShooter() == bukkitPlayer) {
        pearl.setShooter(null);
      }
    }
  }
}

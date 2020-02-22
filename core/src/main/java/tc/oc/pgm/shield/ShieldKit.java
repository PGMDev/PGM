package tc.oc.pgm.shield;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.AbstractKit;

public class ShieldKit extends AbstractKit {

  final ShieldParameters parameters;

  public ShieldKit(ShieldParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  protected void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    player.getMatch().needModule(ShieldMatchModule.class).applyShield(player, parameters);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    player.getMatch().needModule(ShieldMatchModule.class).removeShield(player);
  }
}

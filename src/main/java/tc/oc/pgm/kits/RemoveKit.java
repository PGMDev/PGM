package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class RemoveKit extends AbstractKit {
  private final Kit kit;

  public RemoveKit(Kit kit) {
    this.kit = kit;
  }

  public Kit getKit() {
    return kit;
  }

  @Override
  protected void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    kit.remove(player);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    player.applyKit(kit, false);
  }
}

package tc.oc.pgm.kits;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class KnockbackReductionKit extends AbstractKit {
  private final float knockbackReduction;

  public KnockbackReductionKit(float reduction) {
    this.knockbackReduction = reduction;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    PLAYER_UTILS.setKnockbackReduction(player.getBukkit(), this.knockbackReduction);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    PLAYER_UTILS.setKnockbackReduction(player.getBukkit(), 0);
  }
}

package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.MiscUtils;

public class KnockbackReductionKit extends AbstractKit {
  private final float knockbackReduction;

  public KnockbackReductionKit(float reduction) {
    this.knockbackReduction = reduction;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    MiscUtils.INSTANCE.setKnockbackReduction(player.getBukkit(), this.knockbackReduction);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    MiscUtils.INSTANCE.setKnockbackReduction(player.getBukkit(), 0);
  }
}

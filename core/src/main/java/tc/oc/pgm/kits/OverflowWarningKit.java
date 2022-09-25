package tc.oc.pgm.kits;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class OverflowWarningKit extends AbstractKit {

  private final Component warning;

  public OverflowWarningKit(Component warning) {
    this.warning = warning;
  }

  @Override
  protected void applyPostEvent(
      MatchPlayer player, boolean force, List<ItemStack> displacedItems) {}

  @Override
  public void applyLeftover(MatchPlayer player, List<ItemStack> leftover) {
    player.sendWarning(warning);
  }
}

package tc.oc.pgm.filters;

import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class CarryingItemFilter extends ParticipantItemFilter {
  public CarryingItemFilter(ItemStack base) {
    super(base);
  }

  @Override
  protected ItemStack[] getItems(MatchPlayer player) {
    return player.getBukkit().getInventory().getContents();
  }
}

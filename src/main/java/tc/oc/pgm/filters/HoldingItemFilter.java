package tc.oc.pgm.filters;

import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.match.MatchPlayer;

public class HoldingItemFilter extends ParticipantItemFilter {
  public HoldingItemFilter(ItemStack base) {
    super(base);
  }

  @Override
  protected ItemStack[] getItems(MatchPlayer player) {
    return new ItemStack[] {player.getBukkit().getItemInHand()};
  }
}

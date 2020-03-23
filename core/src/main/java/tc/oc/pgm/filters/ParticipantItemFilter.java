package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public abstract class ParticipantItemFilter extends ParticipantFilter {
  protected final ItemStack base;

  public ParticipantItemFilter(ItemStack base) {
    this.base = Preconditions.checkNotNull(base, "item").clone();
    this.base.setDurability((short) 0); // Filter ignores durability
  }

  protected abstract ItemStack[] getItems(MatchPlayer player);

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    for (ItemStack item : getItems(player)) {
      if (item == null) continue;

      item = item.clone();
      item.setDurability((short) 0);
      if (this.base.isSimilar(item) && item.getAmount() >= base.getAmount()) {
        // Match if items stack (ignoring durability) and player's stack is
        // at least as big as the filter's.
        return QueryResponse.ALLOW;
      }
    }

    return QueryResponse.DENY;
  }
}

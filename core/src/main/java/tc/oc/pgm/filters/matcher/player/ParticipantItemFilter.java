package tc.oc.pgm.filters.matcher.player;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.ItemMatcher;

public abstract class ParticipantItemFilter extends ParticipantFilter {
  protected final ItemMatcher matcher;

  public ParticipantItemFilter(ItemMatcher matcher) {
    this.matcher = assertNotNull(matcher, "item");
  }

  protected abstract ItemStack[] getItems(MatchPlayer player);

  @Override
  public boolean matches(PlayerQuery query, MatchPlayer player) {
    for (ItemStack item : getItems(player)) {
      if (item == null) continue;
      if (matcher.matches(item)) return true;
    }
    return false;
  }
}

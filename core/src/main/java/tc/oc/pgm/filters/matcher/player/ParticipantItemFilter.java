package tc.oc.pgm.filters.matcher.player;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.ItemMatcher;

public abstract class ParticipantItemFilter extends ParticipantFilter {
  protected final ItemMatcher matcher;

  public ParticipantItemFilter(ItemMatcher matcher) {
    this.matcher = assertNotNull(matcher, "item");
  }

  protected abstract Stream<ItemStack> getItems(MatchPlayer player);

  @Override
  public boolean matches(PlayerQuery query, MatchPlayer player) {
    return getItems(player).filter(Objects::nonNull).anyMatch(matcher::matches);
  }
}

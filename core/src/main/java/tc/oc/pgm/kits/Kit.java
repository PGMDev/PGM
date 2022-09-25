package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.player.MatchPlayer;

public interface Kit extends Action<MatchPlayer> {

  /**
   * Apply this kit to the given player. If force is true, the player's state is made to match the
   * kit as strictly as possible, otherwise the kit may be given to the player in a way that is more
   * in their best interest. Subclasses will interpret these concepts in their own way.
   *
   * <p>A mutable List must be given, to which the Kit may add ItemStacks that could not be applied
   * normally, because the player's inventory was full. These stacks will be given to the player
   * using the natural give algorithm after ALL kits have been applied. This phase must be deferred
   * in this way so that overflow from one kit does not displace stacks in another kit applied
   * simultaneously. In this way, the number of stacks that go to their proper slots is maximized.
   */
  void apply(MatchPlayer player, boolean force, List<ItemStack> displacedItems);

  /**
   * Do whatever is necessary with leftover items that couldn't make it into the players' inventory,
   * because it was full. This is always called after apply, and trying to manually add items in
   * displacedItems to the player's inventory.
   *
   * <p>This method allows the kit to prioritize items, drop them on the ground, or warn the user
   * for example.
   *
   * @param player The player the kit is being given
   * @param leftover The leftover items that couldn't fit the player inventory
   */
  void applyLeftover(MatchPlayer player, List<ItemStack> leftover);

  void remove(MatchPlayer player);

  boolean isRemovable();

  @Override
  default Class<MatchPlayer> getScope() {
    return MatchPlayer.class;
  }

  @Override
  default void trigger(MatchPlayer player) {
    player.applyKit(this, false);
  }

  @Override
  default void untrigger(MatchPlayer player) {
    if (isRemovable()) remove(player);
  }
}

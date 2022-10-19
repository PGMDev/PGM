package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * When applied, fires an {@link ApplyKitEvent} and then calls {@link #applyPostEvent} unless the
 * event was cancelled.
 */
public abstract class AbstractKit implements KitDefinition {
  @Override
  public void apply(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    ApplyKitEvent event = new ApplyKitEvent(player, this, force);
    player.getMatch().callEvent(event);
    if (!event.isCancelled()) {
      this.applyPostEvent(player, event.isForce(), displacedItems);
    }
  }

  @Override
  public void applyLeftover(MatchPlayer player, List<ItemStack> leftover) {}

  @Override
  public void remove(MatchPlayer player) {
    throw new UnsupportedOperationException(this + " is not removable");
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  protected abstract void applyPostEvent(
      MatchPlayer player, boolean force, List<ItemStack> displacedItems);
}

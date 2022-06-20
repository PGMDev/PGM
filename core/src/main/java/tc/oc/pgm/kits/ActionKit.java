package tc.oc.pgm.kits;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.action.Action;

public class ActionKit extends AbstractKit {

  private final ImmutableList<Action<? super MatchPlayer>> actions;

  public ActionKit(ImmutableList<Action<? super MatchPlayer>> actions) {
    this.actions = actions;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    for (Action<? super MatchPlayer> t : actions) {
      t.trigger(player);
    }
  }
}

package tc.oc.pgm.kits;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;

public class TriggerKit extends AbstractKit {

  private final ImmutableList<Trigger<? super MatchPlayer>> triggers;

  public TriggerKit(ImmutableList<Trigger<? super MatchPlayer>> triggers) {
    this.triggers = triggers;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    for (Trigger<? super MatchPlayer> t : triggers) {
      t.trigger(player);
    }
  }
}

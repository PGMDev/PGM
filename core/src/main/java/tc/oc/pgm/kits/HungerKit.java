package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

public class HungerKit extends AbstractKit {
  @Nullable protected final Float saturation;
  @Nullable protected final Integer foodLevel;

  public HungerKit(@Nullable Float saturation, @Nullable Integer foodLevel) {
    this.saturation = saturation;
    this.foodLevel = foodLevel;
  }

  /** The force flag allows the kit to decrease the player's food levels */
  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    if (this.saturation != null
        && (force || player.getBukkit().getSaturation() < this.saturation)) {
      player.getBukkit().setSaturation(this.saturation);
    }

    if (this.foodLevel != null && (force || player.getBukkit().getFoodLevel() < this.foodLevel)) {
      player.getBukkit().setFoodLevel(this.foodLevel);
    }
  }
}

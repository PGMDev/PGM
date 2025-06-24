package tc.oc.pgm.kits;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.List;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.player.MatchPlayer;

public class PotionKit extends AbstractKit {
  protected final Set<PotionEffect> effects;

  public PotionKit(Set<PotionEffect> effects) {
    this.effects = effects;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    var pl = player.getBukkit();
    if (force) {
      for (PotionEffect effect : this.effects) {
        // Forced potion eff with duration = 0 is used to remove effects, however in modern versions
        // due to allowing multiple of the same effect, they aren't removed.
        // This makes the behavior explicit that forcing an effect with duration 0 removes it.
        if (effect.getDuration() != 0) pl.addPotionEffect(effect, true);
        else pl.removePotionEffect(effect.getType());
      }
    } else {
      pl.addPotionEffects(this.effects);
    }

    // No swirls by default, KitNode can re-enable them if it so desires
    PLAYER_UTILS.setPotionParticles(pl, false);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  @Override
  public void remove(MatchPlayer player) {
    for (PotionEffect effect : effects) {
      player.getBukkit().removePotionEffect(effect.getType());
    }
  }
}

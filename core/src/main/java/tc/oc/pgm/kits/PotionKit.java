package tc.oc.pgm.kits;

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
    if (force) {
      for (PotionEffect effect : this.effects) {
        player.getBukkit().addPotionEffect(effect, true);
      }
    } else {
      player.getBukkit().addPotionEffects(this.effects);
    }

    // No swirls by default, KitNode can re-enable them if it so desires
    player.getBukkit().setPotionParticles(false);
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

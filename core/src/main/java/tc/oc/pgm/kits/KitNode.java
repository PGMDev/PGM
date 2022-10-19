package tc.oc.pgm.kits;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.util.nms.NMSHacks;

public class KitNode extends AbstractKit {
  private final List<Kit> kits;
  private final Filter filter;
  private final @Nullable Boolean force;
  private final @Nullable Boolean potionParticles;

  public KitNode(
      List<Kit> kits, Filter filter, @Nullable Boolean force, @Nullable Boolean potionParticles) {
    this.kits = kits;
    this.filter = filter;
    this.force = force;
    this.potionParticles = potionParticles;
  }

  public static Kit of(Kit... kits) {
    if (kits.length == 0) return EMPTY;
    return new KitNode(Arrays.asList(kits), StaticFilter.ALLOW, null, null);
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    if (this.filter.query(player).isAllowed()) {
      for (Kit kit : this.kits) {
        kit.apply(player, this.force != null ? this.force : force, displacedItems);
      }

      if (this.potionParticles != null) {
        NMSHacks.setPotionParticles(player.getBukkit(), this.potionParticles);
      }
    }
  }

  @Override
  public void applyLeftover(MatchPlayer player, List<ItemStack> leftover) {
    for (Kit kit : this.kits) kit.applyLeftover(player, leftover);
  }

  @Override
  public boolean isRemovable() {
    for (Kit kit : kits) {
      if (!kit.isRemovable()) return false;
    }
    return true;
  }

  @Override
  public void untrigger(MatchPlayer player) {
    for (Kit kit : kits) {
      if (kit.isRemovable()) kit.remove(player);
    }
  }

  @Override
  public void remove(MatchPlayer player) {
    for (Kit kit : kits) {
      kit.remove(player);
    }
  }

  public static final KitNode EMPTY =
      new KitNode(Collections.<Kit>emptyList(), StaticFilter.ALLOW, null, null);
}

package tc.oc.pgm.platform.sportpaper;

import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;

@NullMarked
public class SpBlockTransformListener extends BlockTransformListener {

  /** A burned block and the fuel it held */
  private record Burned(UUID world, int x, int y, int z, Material type) {
    static Burned of(Block block) {
      return new Burned(
          block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), block.getType());
    }

    boolean matches(Block block) {
      return block.getX() == x
          && block.getY() == y
          && block.getZ() == z
          && block.getType() == type
          && block.getWorld().getUID().equals(world);
    }
  }

  // Where an allowed BlockBurnEvent has just consumed a block, if the fade that may follow it has
  // not arrived yet.
  private @Nullable Burned burned;

  public SpBlockTransformListener(Plugin plugin) {
    super(plugin);
  }

  @Override
  @EventWrapper
  public void onBlockForm(final BlockFormEvent event) {
    // Handled by onBlockFade
    if (event.getBlock().getType() == Material.ICE) return;

    super.onBlockForm(event);
  }

  @Override
  @EventWrapper
  public void onPrimeTNT(final ExplosionPrimeEvent event) {
    if (event instanceof ExplosionPrimeByEntityEvent prime) {
      // A flaming arrow fires an EntityChangeBlockEvent before this (BlockTNT.java:127-143), which
      // already reports the removal with the arrow's owner.
      if (prime.getPrimer() instanceof Projectile) return;
    } else if (burned != null && burned.matches(event.getEntity().getLocation().getBlock())) {
      // Handled by onBlockBurn: burning TNT is primed straight after its burn, while the block is
      // still in place (BlockFire.java:259-271)
      return;
    }

    super.onPrimeTNT(event);
  }

  @Override
  @EventWrapper
  public void onBlockFade(final BlockFadeEvent event) {
    // Avoid SportPaper-specific duplicate transforms (BlockFire.java:259,285,372-376)
    // The fade can only come straight after its burn, so any fade uses up what the burn left.
    Burned lastBurned = burned;
    burned = null;
    if (lastBurned != null && lastBurned.matches(event.getBlock())) return;

    super.onBlockFade(event);
  }

  @Override
  protected void finishCauseEvent(Event causeEvent, List<BlockTransformEvent> wrapperEvents) {
    super.finishCauseEvent(causeEvent, wrapperEvents);
    burned = causeEvent instanceof BlockBurnEvent burn && isAllowed(burn, wrapperEvents)
        ? Burned.of(burn.getBlock())
        : null;
  }

  private static boolean isAllowed(BlockBurnEvent burn, List<BlockTransformEvent> wrapperEvents) {
    if (burn.isCancelled()) return false;
    for (BlockTransformEvent wrapper : wrapperEvents) {
      if (wrapper.isCancelled() && wrapper.getBlock().equals(burn.getBlock())) return false;
    }
    return true;
  }
}

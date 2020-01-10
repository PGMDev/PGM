package tc.oc.pgm.destroyable;

import java.util.Collection;
import org.bukkit.block.Block;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.material.MaterialData;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.modes.ObjectiveModeChangeEvent;

public class DestroyableMatchModule implements MatchModule, Listener {
  protected final Match match;
  protected final Collection<Destroyable> destroyables;

  public DestroyableMatchModule(Match match, Collection<Destroyable> destroyables) {
    this.match = match;
    this.destroyables = destroyables;
  }

  public Collection<Destroyable> getDestroyables() {
    return destroyables;
  }

  private boolean anyDestroyableAffected(BlockTransformEvent event) {
    for (Destroyable destroyable : this.destroyables) {
      if (!destroyable.isDestroyed()
          && destroyable.getBlockRegion().contains(event.getNewState())) {
        return true;
      }
    }
    return false;
  }

  /**
   * This handler only checks to see if the event should be cancelled. It does not change the state
   * of any Destroyables.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void testBlockChange(BlockTransformEvent event) {
    if (this.match.getWorld() != event.getWorld() || !this.anyDestroyableAffected(event)) {
      return;
    }

    // This is a temp fix until there is a tracker for placed minecarts (only dispensed are tracked
    // right now)
    if ((event.getCause() instanceof EntityExplodeEvent
            && ((EntityExplodeEvent) event.getCause()).getEntity() instanceof ExplosiveMinecart)
        || event.getCause() instanceof BlockPistonExtendEvent
        || event.getCause() instanceof BlockPistonRetractEvent) {

      event.setCancelled(true);
      return;
    }

    for (Destroyable destroyable : this.destroyables) {
      String reasonKey =
          destroyable.testBlockChange(
              event.getOldState(),
              event.getNewState(),
              ParticipantBlockTransformEvent.getPlayerState(event));
      if (reasonKey != null) {
        event.setCancelled(true, new PersonalizedTranslatable(reasonKey));
        return;
      }
    }
  }

  /**
   * This handler updates the state of Destroyables to reflect the block change, which is now
   * definitely happening since this is listening on MONITOR.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleBlockChange(BlockTransformEvent event) {
    if (this.match.getWorld() != event.getWorld() || !this.anyDestroyableAffected(event)) {
      return;
    }

    for (Destroyable destroyable : this.destroyables) {
      destroyable.handleBlockChange(
          event.getOldState(),
          event.getNewState(),
          ParticipantBlockTransformEvent.getPlayerState(event));
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void processBlockDamage(BlockDamageEvent event) {
    if (this.match.getWorld() != event.getBlock().getWorld()) return;

    Block block = event.getBlock();
    MaterialData material = block.getState().getData();
    MatchPlayer player = this.match.getPlayer(event.getPlayer());

    for (Destroyable destroyable : this.destroyables) {
      if (player != null
          && player.getParty() == destroyable.getOwner()
          && !destroyable.isDestroyed()
          && destroyable.getBlockRegion().contains(block)
          && destroyable.hasMaterial(material)) {

        event.setCancelled(true);
        player.sendWarning("You may not damage your own objective.", true);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onObjectiveModeSwitch(final ObjectiveModeChangeEvent event) {
    for (Destroyable destroyable : this.destroyables) {
      if (destroyable.isAffectedByModeChanges()) {
        double oldCompletion = destroyable.getCompletion();
        destroyable.replaceBlocks(event.getMode().getMaterialData());
        if (oldCompletion != destroyable.getCompletion()) {
          // Multi-stage destroyables can have their total completion changed by this
          this.match.callEvent(new DestroyableHealthChangeEvent(this.match, destroyable, null));
        }
      }
    }
  }
}

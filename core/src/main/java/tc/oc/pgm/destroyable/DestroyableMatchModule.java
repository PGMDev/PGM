package tc.oc.pgm.destroyable;

import static net.kyori.adventure.text.Component.translatable;

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
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.modes.ObjectiveModeChangeEvent;
import tc.oc.pgm.util.material.MaterialData;

@ListenerScope(MatchScope.RUNNING)
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

    ParticipantState player = ParticipantBlockTransformEvent.getPlayerState(event);

    if (event.getCause() instanceof EntityExplodeEvent
        && ((EntityExplodeEvent) event.getCause()).getEntity() instanceof ExplosiveMinecart) {
      // If the platform doesn't provide enough data to tell
      // who owns the TNT minecart that blew up the destroyable,
      // cancel the event to prevent possible team griefing
      if (player == null) {
        event.setCancelled(true);
        return;
      }
    } else if (event.getCause() instanceof BlockPistonExtendEvent
        || event.getCause() instanceof BlockPistonRetractEvent) {

      event.setCancelled(true);
      return;
    }

    for (Destroyable destroyable : this.destroyables) {
      String reasonKey =
          destroyable.testBlockChange(event.getOldState(), event.getNewState(), player);
      if (reasonKey != null) {
        event.setCancelled(translatable(reasonKey, destroyable.getComponentName()));
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
    MaterialData material = MaterialData.block(block.getState());
    MatchPlayer player = this.match.getPlayer(event.getPlayer());

    for (Destroyable destroyable : this.destroyables) {
      if (player != null
          && player.getParty() == destroyable.getOwner()
          && !destroyable.isDestroyed()
          && destroyable.getBlockRegion().contains(block)
          && destroyable.hasMaterial(material)) {

        event.setCancelled(true);
        player.sendWarning(translatable("objective.damageOwn", destroyable.getComponentName()));
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onObjectiveModeSwitch(final ObjectiveModeChangeEvent event) {
    for (Destroyable destroyable : this.destroyables) {
      if (destroyable.isAffectedBy(event.getMode())) {
        double oldCompletion = destroyable.getCompletion();
        destroyable.replaceBlocks(event.getMode().getMaterialData());
        // if at least one of the destroyables are visible, the mode change message will be sent
        if (destroyable.hasShowOption(ShowOption.SHOW_MESSAGES)) {
          event.setVisible(true);
        }
        if (oldCompletion != destroyable.getCompletion()) {
          // Multi-stage destroyables can have their total completion changed by this
          this.match.callEvent(new DestroyableHealthChangeEvent(this.match, destroyable, null));
        }
      }
    }
  }
}

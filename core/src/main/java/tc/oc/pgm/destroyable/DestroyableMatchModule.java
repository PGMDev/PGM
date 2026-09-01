package tc.oc.pgm.destroyable;

import static net.kyori.adventure.text.Component.translatable;

import java.util.Collection;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.MaterialData;

@NullMarked
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

  private boolean anyDestroyableAffected(long pos) {
    for (Destroyable destroyable : this.destroyables) {
      if (!destroyable.isDestroyed() && destroyable.getBlockRegion().containsPos(pos)) {
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
    if (this.match.getWorld() != event.getWorld()) return;

    long pos = event.getPos();
    if (!this.anyDestroyableAffected(pos)) return;

    ParticipantState player = ParticipantBlockTransformEvent.getPlayerState(event);
    if (shouldCancel(event.getCause(), player)) {
      event.setCancelled(true);
      return;
    }

    BlockState oldState = event.getOldState();
    BlockState newState = event.getNewState();

    for (Destroyable destroyable : this.destroyables) {
      String reasonKey = destroyable.testBlockChange(oldState, newState, player, pos);
      if (reasonKey != null) {
        event.setCancelled(translatable(reasonKey, destroyable.getComponentName()));
        return;
      }
    }
  }

  private boolean shouldCancel(@Nullable Event cause, @Nullable ParticipantState player) {
    // If the platform doesn't provide enough data to tell who owns the
    // TNT minecart, cancel the event to prevent possible team griefing
    return cause instanceof BlockPistonExtendEvent
        || cause instanceof BlockPistonRetractEvent
        || (cause instanceof EntityExplodeEvent explosion
            && explosion.getEntity() instanceof ExplosiveMinecart
            && player == null);
  }

  /**
   * This handler updates the state of Destroyables to reflect the block change, which is now
   * definitely happening since this is listening on MONITOR.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleBlockChange(BlockTransformEvent event) {
    if (this.match.getWorld() != event.getWorld()) return;

    long pos = event.getPos();
    if (!this.anyDestroyableAffected(pos)) return;

    BlockState oldState = event.getOldState();
    BlockState newState = event.getNewState();
    ParticipantState player = ParticipantBlockTransformEvent.getPlayerState(event);

    for (Destroyable destroyable : this.destroyables) {
      destroyable.handleBlockChange(oldState, newState, player, pos);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void processBlockDamage(BlockDamageEvent event) {
    if (this.match.getWorld() != event.getBlock().getWorld()) return;

    Block block = event.getBlock();
    MaterialData material = MaterialData.block(block.getState());
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null) return;

    long pos = BlockVectors.encodePos(block);
    for (Destroyable destroyable : this.destroyables) {
      if (player.getParty() == destroyable.getOwner()
          && !destroyable.isDestroyed()
          && destroyable.getBlockRegion().containsPos(pos)
          && destroyable.hasMaterial(material)) {

        event.setCancelled(true);
        player.sendWarning(translatable("objective.damageOwn", destroyable.getComponentName()));
        break;
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

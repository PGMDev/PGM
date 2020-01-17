package tc.oc.pgm.core;

import static tc.oc.pgm.api.map.MapProtos.MODES_IMPLEMENTATION_VERSION;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;
import tc.oc.block.BlockVectors;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.modes.ObjectiveModeChangeEvent;

public class CoreMatchModule implements MatchModule, Listener {

  protected final Match match;
  protected final List<Core> cores;

  public CoreMatchModule(Match match, List<Core> cores) {
    this.match = match;
    this.cores = cores;
  }

  @Override
  public void enable() {
    if (this.match.getMap().getProto().isOlderThan(MODES_IMPLEMENTATION_VERSION)) {
      CoreConvertMonitor ccm = new CoreConvertMonitor(this);

      match.getScheduler(MatchScope.RUNNING).runTaskLater(15 * 60 * 20, ccm); // 15 minutes
      match.getScheduler(MatchScope.RUNNING).runTaskLater(20 * 60 * 20, ccm); // 20 minutes
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void leakCheck(final BlockTransformEvent event) {
    if (event.getWorld() != this.match.getWorld()) return;

    if (event.getNewState().getType() == Material.STATIONARY_LAVA) {
      Vector blockVector = BlockVectors.center(event.getNewState()).toVector();
      for (Core core : this.cores) {
        if (!core.hasLeaked() && core.getLeakRegion().contains(blockVector)) {
          // core has leaked
          core.markLeaked();
          this.match.callEvent(new CoreLeakEvent(this.match, core, event.getNewState()));
          this.match.callEvent(
              new GoalCompleteEvent(
                  this.match, core, core.getOwner(), false, core.getContributions()));
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void breakCheck(final BlockTransformEvent event) {
    if (event.getWorld() != this.match.getWorld()) return;
    ParticipantState player = ParticipantBlockTransformEvent.getPlayerState(event);

    Vector blockVector = BlockVectors.center(event.getNewState()).toVector();

    for (Core core : this.cores) {
      if (!core.hasLeaked() && core.getCasingRegion().contains(blockVector)) {
        if (event.getNewState().getType() == Material.AIR) {
          if (player != null) {
            Competitor team = player.getParty();

            if (team == core.getOwner()) {
              event.setCancelled(true, new PersonalizedTranslatable("match.core.damageOwn"));
            } else if (event.getOldState().getData().equals(core.getMaterial())) {
              this.match.callEvent(new CoreBlockBreakEvent(core, player, event.getOldState()));
              core.touch(player);

              // Note: team may not have touched a broken core if a different team broke it
              if (!core.isCompleted(team) && !core.hasTouched(team)) {
                this.match.callEvent(new GoalStatusChangeEvent(this.match, core));
              }
            }
          } else if (event.getCause() instanceof EntityExplodeEvent) {
            // this is a temp fix until there is a tracker for placed minecarts (only dispensed are
            // tracked right now)
            if (((EntityExplodeEvent) event.getCause()).getEntity() instanceof ExplosiveMinecart) {
              event.setCancelled(true);
            }
          } else if (event.getCause() instanceof BlockPistonRetractEvent) {
            event.setCancelled(true);
          }
        } else if (event.getCause() instanceof BlockPistonExtendEvent) {
          event.setCancelled(true);
        } else if (event.getCause() instanceof BlockDispenseEvent) {
          event.setCancelled(true);
        }
        break;
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void damageCheck(BlockDamageEvent event) {
    Block block = event.getBlock();
    if (block.getWorld() != this.match.getWorld()) return;
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    Vector center = BlockVectors.center(block).toVector();

    for (Core core : this.cores) {
      if (!core.hasLeaked()
          && core.getCasingRegion().contains(center)
          && player.getParty() == core.getOwner()) {
        event.setCancelled(true);
        player.sendWarning(
            AllTranslations.get().translate("match.core.damageOwn", player.getBukkit()), true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void lavaProtection(final BlockTransformEvent event) {
    if (event.getWorld() != this.match.getWorld()) return;

    Vector blockVector = BlockVectors.center(event.getNewState()).toVector();
    for (Core core : this.cores) {
      if (core.getLavaRegion().contains(blockVector)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onObjectiveModeSwitch(final ObjectiveModeChangeEvent event) {
    for (Core core : this.cores) {
      if (core.isAffectedByModeChanges()) {
        core.replaceBlocks(event.getMode().getMaterialData());
      }
    }
  }
}

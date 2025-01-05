package tc.oc.pgm.core;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.api.map.MapProtos.MODES_IMPLEMENTATION_VERSION;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.modes.ObjectiveModeChangeEvent;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.Materials;

@ListenerScope(MatchScope.RUNNING)
public class CoreMatchModule implements MatchModule, Listener {

  protected final Match match;
  protected final List<Core> cores;

  public CoreMatchModule(Match match, List<Core> cores) {
    this.match = match;
    this.cores = cores;
  }

  public List<Core> getCores() {
    return this.cores;
  }

  @Override
  public void enable() {
    if (this.match.getMap().getProto().isOlderThan(MODES_IMPLEMENTATION_VERSION)) {
      CoreConvertMonitor ccm = new CoreConvertMonitor(this);

      match.getExecutor(MatchScope.RUNNING).schedule(ccm, 15, TimeUnit.MINUTES);
      match.getExecutor(MatchScope.RUNNING).schedule(ccm, 20, TimeUnit.MINUTES);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void leakCheck(final BlockTransformEvent event) {
    if (event.getWorld() != this.match.getWorld()) return;

    if (Materials.isLava(event.getNewState().getType())) {
      var blockVector = BlockVectors.center(event.getNewState());
      // Vector ensuring it's inside leak region if it's above
      var minVector = blockVector.clone();
      minVector.setY(0.5);
      for (Core core : this.cores) {
        if (core.hasLeaked() || !core.getLeakRegion().contains(minVector)) continue;

        if (core.updateLeak(blockVector.getBlockY())) {
          this.match.callEvent(new GoalStatusChangeEvent(this.match, core));
        }

        if (core.getLeakRegion().contains(blockVector)) {
          // core has leaked
          core.markLeaked();
          this.match.callEvent(new CoreLeakEvent(this.match, core, event.getNewState()));
          this.match.callEvent(new GoalCompleteEvent(
              this.match, core, core.getOwner(), false, core.getContributions()));
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void breakCheck(final BlockTransformEvent event) {
    if (event.getWorld() != this.match.getWorld()) return;
    ParticipantState player = ParticipantBlockTransformEvent.getPlayerState(event);

    var blockVector = BlockVectors.center(event.getNewState());

    for (Core core : this.cores) {
      if (!core.hasLeaked() && core.getCasingRegion().contains(blockVector)) {
        if (event.getNewState().getType() == Material.AIR) {
          if (player != null) {
            Competitor team = player.getParty();

            if (team == core.getOwner()) {
              event.setCancelled(translatable("objective.damageOwn", core.getComponentName()));
            } else if (core.isCoreMaterial(MaterialData.block(event.getOldState()))) {
              this.match.callEvent(new CoreBlockBreakEvent(core, player, event.getOldState()));
              core.touch(player);

              // Note: team may not have touched a broken core if a different team broke it
              if (!core.isCompleted(team) && !core.hasTouched(team)) {
                this.match.callEvent(new GoalStatusChangeEvent(this.match, core));
              }
            }
          } else if (event.getCause() instanceof EntityExplodeEvent) {
            // If the platform doesn't provide enough data to tell
            // who owns the TNT minecart that blew up the core, cancel the
            // event to prevent possible team griefing
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
    var center = BlockVectors.center(block);

    for (Core core : this.cores) {
      if (!core.hasLeaked()
          && core.getCasingRegion().contains(center)
          && player.getParty() == core.getOwner()) {
        event.setCancelled(true);
        player.sendWarning(translatable("objective.damageOwn", core.getComponentName()));
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void lavaProtection(final BlockTransformEvent event) {
    if (event.getWorld() != this.match.getWorld()) return;

    var blockVector = BlockVectors.center(event.getNewState());
    for (Core core : this.cores) {
      if (core.getLavaRegion().contains(blockVector)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onObjectiveModeSwitch(final ObjectiveModeChangeEvent event) {
    for (Core core : this.cores) {
      if (core.isAffectedBy(event.getMode())) {
        core.replaceBlocks(event.getMode().getMaterialData());
        // if at least one of the cores are visible, the mode change message will be sent
        if (core.hasShowOption(ShowOption.SHOW_MESSAGES)) {
          event.setVisible(true);
        }
      }
    }
  }
}

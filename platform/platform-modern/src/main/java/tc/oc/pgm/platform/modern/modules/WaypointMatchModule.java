package tc.oc.pgm.platform.modern.modules;

import java.util.Optional;
import net.minecraft.server.waypoints.ServerWaypointManager;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.platform.modern.modules.waypoints.PGMWaypointTransmitter;
import tc.oc.pgm.platform.modern.modules.waypoints.Waypoints;
import tc.oc.pgm.score.ScoreBox;
import tc.oc.pgm.score.ScoreMatchModule;

@ListenerScope(MatchScope.LOADED)
public class WaypointMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ServerWaypointManager waypointManager;

  public WaypointMatchModule(Match match) {
    this.match = match;
    this.waypointManager = ((CraftWorld) match.getWorld()).getHandle().getWaypointManager();
  }

  private void track(PGMWaypointTransmitter transmitter) {
    if (transmitter == null) return;

    waypointManager.trackWaypoint(transmitter);
    if (transmitter instanceof Tickable t) {
      // Initial update before match start
      t.tick(match, match.getTick());
      match.addTickable(t, MatchScope.RUNNING);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void afterLoad(MatchLoadEvent event) {
    match.moduleOptional(GoalMatchModule.class).ifPresent(gmm -> {
      for (Goal<?> goal : gmm.getGoals()) {
        track(Waypoints.from(goal));
      }
    });
    match.moduleOptional(ScoreMatchModule.class).ifPresent(smm -> {
      for (ScoreBox scoreBox : smm.getScoreBoxes()) {
        scoreBox
            .getOwner(match)
            .ifPresent(owner -> track(Waypoints.immutable(
                Waypoints.toBlockPos(match, scoreBox.getRegion()),
                owner.getFullColor().asRGB())));
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    setPlayerWaypoint(event.getPlayer(), null);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagPickup(FlagStateChangeEvent event) {
    if (event.getOldState() instanceof Carried c) setPlayerWaypoint(c.getCarrier(), null);
    if (event.getNewState() instanceof Carried c)
      setPlayerWaypoint(c.getCarrier(), event.getFlag().getColor());
  }

  private void setPlayerWaypoint(MatchPlayer player, Color color) {
    var attr = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
    if (attr == null) return;
    if (color != null) {
      var nmsPlayer = ((CraftPlayer) player.getBukkit()).getHandle();
      // Ensures they're newly registered so the color updates
      waypointManager.untrackWaypoint(nmsPlayer);
      nmsPlayer.waypointIcon().color = Optional.of(color.asRGB());
      attr.setBaseValue(256);
    } else {
      attr.setBaseValue(0);
    }
  }
}

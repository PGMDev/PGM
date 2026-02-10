package tc.oc.pgm.platform.modern.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.platform.modern.modules.waypoints.Waypoints;

@ListenerScope(MatchScope.LOADED)
public class WaypointMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<Goal<?>, WaypointTransmitter> trackedWaypoints;
  private final ServerWaypointManager waypointManager;

  public WaypointMatchModule(Match match) {
    this.match = match;
    this.trackedWaypoints = new HashMap<>();
    this.waypointManager = ((CraftWorld) match.getWorld()).getHandle().getWaypointManager();
  }

  private void track(Goal<?> goal, WaypointTransmitter transmitter) {
    if (transmitter == null) return;

    waypointManager.trackWaypoint(transmitter);
    trackedWaypoints.put(goal, transmitter);
  }

  private void update(Goal<?> goal) {
    var waypoint = trackedWaypoints.get(goal);
    if (waypoint != null) waypointManager.updateWaypoint(waypoint);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void afterLoad(MatchLoadEvent event) {
    match.moduleOptional(GoalMatchModule.class).ifPresent(gmm -> {
      for (Goal<?> goal : gmm.getGoals()) {
        track(goal, Waypoints.from(goal));
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    setPlayerWaypoint(event.getPlayer(), false, 0);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagPickup(FlagStateChangeEvent event) {
    if (event.getOldState() instanceof Carried c) setPlayerWaypoint(c.getCarrier(), false, 0);
    if (event.getNewState() instanceof Carried c)
      setPlayerWaypoint(c.getCarrier(), true, event.getFlag().getColor().asRGB());
    update(event.getFlag());
  }

  private void setPlayerWaypoint(MatchPlayer player, boolean enabled, int color) {
    var attr = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
    if (attr == null) return;
    if (enabled) {
      ((CraftPlayer) player.getBukkit()).getHandle().waypointIcon().color = Optional.of(color);
      attr.setBaseValue(256);
    } else {
      attr.setBaseValue(0);
    }
  }
}

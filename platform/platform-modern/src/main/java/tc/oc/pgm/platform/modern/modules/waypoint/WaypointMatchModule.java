package tc.oc.pgm.platform.modern.modules.waypoint;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;
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
import tc.oc.pgm.platform.modern.modules.waypoint.types.Waypoints;
import tc.oc.pgm.score.ScoreBox;
import tc.oc.pgm.score.ScoreMatchModule;

@ListenerScope(MatchScope.LOADED)
public class WaypointMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ServerWaypointManager waypointManager;
  private final Map<String, WaypointDefinition> waypointDefinitions;

  public WaypointMatchModule(Match match, Map<String, WaypointDefinition> waypointDefinitions) {
    this.match = match;
    this.waypointDefinitions = waypointDefinitions;
    this.waypointManager = ((CraftWorld) match.getWorld()).getHandle().getWaypointManager();
  }

  public void track(WaypointTransmitter transmitter) {
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
    removePlayerWaypoint(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagPickup(FlagStateChangeEvent event) {
    if (event.getOldState() instanceof Carried c) removePlayerWaypoint(c.getCarrier());
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

  public void untrack(WaypointTransmitter transmitter) {
    waypointManager.untrackWaypoint(transmitter);
  }

  private void applyPlayerWaypointTracking(MatchPlayer player, @Nullable WaypointDefinition def) {
    var attr = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
    if (attr == null) return;
    var nmsPlayer = ((CraftPlayer) player.getBukkit()).getHandle();

    waypointManager.untrackWaypoint(nmsPlayer);

    if (def != null) {
      nmsPlayer.waypointIcon().style = def.getStyle();
      nmsPlayer.waypointIcon().color = Optional.of(def.getColor().asRGB());
      float range = def.getTransmitRange() != null ? def.getTransmitRange() : 256f;
      attr.setBaseValue(range);
      waypointManager.trackWaypoint(nmsPlayer);
    } else {
      nmsPlayer.waypointIcon().style = WaypointStyleAssets.DEFAULT;
      nmsPlayer.waypointIcon().color = Optional.empty();
      attr.setBaseValue(0);
    }
  }

  public void setPlayerWaypoint(MatchPlayer player, WaypointDefinition definition) {
    this.applyPlayerWaypointTracking(player, definition);
  }

  public void removePlayerWaypoint(MatchPlayer player) {
    this.applyPlayerWaypointTracking(player, null);
  }

  public void applyEntityWaypoint(
      org.bukkit.entity.Entity entity, @Nullable WaypointDefinition def) {
    var attr = ((org.bukkit.attribute.Attributable) entity)
        .getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
    if (attr == null) return;
    var nms = ((org.bukkit.craftbukkit.entity.CraftLivingEntity) entity).getHandle();

    waypointManager.untrackWaypoint(nms);

    if (def != null) {
      nms.waypointIcon().style = def.getStyle();
      nms.waypointIcon().color = Optional.of(def.getColor().asRGB());
      attr.setBaseValue(def.getTransmitRange() != null ? def.getTransmitRange() : 256f);
    } else {
      nms.waypointIcon().style = WaypointStyleAssets.DEFAULT;
      nms.waypointIcon().color = Optional.empty();
      attr.setBaseValue(0);
    }
  }
}

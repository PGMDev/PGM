package tc.oc.pgm.platform.modern.action.actions;

import javax.annotation.Nullable;
import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointMatchModule;

public class WaypointAction extends AbstractAction<MatchPlayer> {

  private final @Nullable FeatureReference<WaypointDefinition> waypointRef;
  private final @Nullable Boolean remove;

  public WaypointAction(
      @Nullable FeatureReference<WaypointDefinition> waypointRef, @Nullable Boolean remove) {
    super(MatchPlayer.class);
    this.waypointRef = waypointRef;
    this.remove = remove;
  }

  @Override
  public void trigger(MatchPlayer player) {
    var wmm = player.getMatch().needModule(WaypointMatchModule.class);
    if (waypointRef != null) {
      wmm.setPlayerWaypoint(player, waypointRef.get());
    } else {
      wmm.removePlayerWaypoint(player);
    }
  }
}

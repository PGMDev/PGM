package tc.oc.pgm.platform.modern.modules.waypoints;

import java.util.Optional;
import org.bukkit.Color;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.controlpoint.ControlPoint;

public class ControlPointWaypoint extends GoalWaypoint<ControlPoint> implements Tickable {

  private Competitor lastCompetitor;

  public ControlPointWaypoint(ControlPoint goal) {
    super(goal, Color.WHITE.asRGB());
    setPosition(goal.getCenterPoint());
  }

  @Override
  public void tick(Match match, Tick tick) {
    var comp = goal.getControllingTeam();
    if (this.lastCompetitor != comp) {
      this.lastCompetitor = comp;
      var color = comp != null ? comp.getFullColor() : Color.WHITE;
      waypointIcon().color = Optional.of(color.asRGB());
    }
  }

  @Override
  public boolean isTransmittingWaypoint() {
    return !goal.isCompleted() || !goal.getDefinition().isPermanent();
  }
}

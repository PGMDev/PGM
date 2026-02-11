package tc.oc.pgm.platform.modern.modules.waypoints;

import java.util.Optional;
import org.bukkit.Color;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.payload.Payload;

public class PayloadWaypoint extends GoalWaypoint<Payload> implements Tickable {

  private Competitor lastCompetitor;

  public PayloadWaypoint(Payload goal) {
    super(goal, Color.WHITE.asRGB(), goal.getMinecartUuid());
    // Force a first reload
    goal.shouldDisplay(true);
  }

  @Override
  public void tick(Match match, Tick tick) {
    setPosition(goal.getCenterPoint());

    var comp = goal.getDisplayTeam();
    if (this.lastCompetitor != comp) {
      this.lastCompetitor = comp;
      var color = comp != null ? comp.getFullColor() : Color.WHITE;
      waypointIcon().color = Optional.of(color.asRGB());
    }
  }

  @Override
  public boolean isTransmittingWaypoint() {
    return super.isTransmittingWaypoint()
        && (!goal.isCompleted() || !goal.getDefinition().isPermanent())
        && goal.shouldDisplay(false);
  }
}

package tc.oc.pgm.platform.modern.modules.waypoint.types;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.state.Carried;

class FlagWaypointTransmitter extends GoalWaypoint<Flag> implements Tickable {

  FlagWaypointTransmitter(Flag flag) {
    super(flag, flag.getDyeColor().getColor().asRGB());
  }

  @Override
  public void tick(Match match, Tick tick) {
    setPosition(goal.getLocation().orElse(null));
  }

  @Override
  public boolean isTransmittingWaypoint() {
    // Carried flags will use the player was waypoint instead, so that movement is smooth
    return super.isTransmittingWaypoint() && !goal.isCurrent(Carried.class);
  }
}

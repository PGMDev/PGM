package tc.oc.pgm.tracker.info;

import static java.util.Objects.requireNonNull;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;

public class GenericFallInfo implements FallInfo {

  private final To to;
  private final Location origin;

  public GenericFallInfo(To to, Location origin) {
    this.to = requireNonNull(to);
    this.origin = requireNonNull(origin);
  }

  public GenericFallInfo(To to, Location location, double distance) {
    this(to, location.clone().add(0, distance, 0));
  }

  @Override
  public From getFrom() {
    return From.GROUND;
  }

  @Override
  public To getTo() {
    return to;
  }

  @Override
  public @Nullable TrackerInfo getCause() {
    return null;
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return null;
  }

  @Override
  public Location getOrigin() {
    return origin;
  }
}

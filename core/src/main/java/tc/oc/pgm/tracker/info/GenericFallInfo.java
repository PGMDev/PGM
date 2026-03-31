package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;

public record GenericFallInfo(To to, Location origin) implements FallInfo {

  public GenericFallInfo {
    assertNotNull(to);
    assertNotNull(origin);
  }

  public GenericFallInfo(To to, Location location, double distance) {
    this(to, location.clone().add(0, distance, 0));
  }

  @Override
  public From from() {
    return From.GROUND;
  }

  @Override
  public @Nullable TrackerInfo cause() {
    return null;
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return null;
  }
}

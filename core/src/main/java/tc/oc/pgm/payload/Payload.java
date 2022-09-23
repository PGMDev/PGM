package tc.oc.pgm.payload;

import java.time.Duration;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.payload.track.Track;

public class Payload extends ControlPoint {

  private static final int PARTICLE_AMOUNT = 6; // 6 particles
  private static final int PARTICLE_ROTATION = 20; // ticks per full rotation

  private final Match match;
  private final PayloadDefinition definition;

  private final Track track;
  private final PayloadRegion region;

  private Competitor dominantTeam;
  private Vector position;

  public Payload(Match match, PayloadDefinition definition) {
    super(match, definition);
    this.match = match;
    this.definition = definition;

    this.track = new Track(match, definition.getLocation());
    this.region = new PayloadRegion(this);
    this.playerTracker.setRegion(region);

    this.position = definition.getLocation();
  }

  @Override
  public void tick(Duration duration) {
    super.tick(duration);

    Vector oldPos = position;
    position = track.getVector(getCompletion());

    if (!oldPos.toBlockVector().equals(position.toBlockVector())) playerTracker.setRegion(region);

    tickDisplay(match.getTick().tick);
  }

  private void tickDisplay(long tick) {
    Color color = dominantTeam == null ? Color.WHITE : dominantTeam.getFullColor();

    Location loc = position.toLocation(match.getWorld()).add(0, 0.5, 0);
    spawnParticle(loc, color);

    double diff = Math.PI * 2 / PARTICLE_AMOUNT;
    double offset = (double) tick * diff / PARTICLE_ROTATION;

    // Each iteration of this loop is one particle
    for (int i = 0; i < PARTICLE_AMOUNT; i++) {
      double angle = i * diff + offset;
      // Height between 0.2 and 0.8
      double height = 0.5d + 0.3d * Math.sin(0.1d * tick + 0.5d * i);

      loc.set(
          definition.getRadius() * Math.cos(angle),
          height,
          definition.getRadius() * Math.sin(angle));
      loc.add(position);
      spawnParticle(loc, color);
    }
  }

  private void spawnParticle(Location loc, Color color) {
    match
        .getWorld()
        .spigot()
        .playEffect(
            loc,
            Effect.COLOURED_DUST,
            0,
            (byte) 0,
            rgbToParticle(color.getRed()),
            rgbToParticle(color.getGreen()),
            rgbToParticle(color.getBlue()),
            1,
            0,
            200);
  }

  private float rgbToParticle(int rgb) {
    return (float) Math.max(0.001, rgb / 255.0);
  }

  @Override
  public PayloadDefinition getDefinition() {
    return (PayloadDefinition) super.getDefinition();
  }

  public Vector getCurrentPosition() {
    return position;
  }

  @Override
  protected void dominate(Competitor dominantTeam, Duration dominantTime, boolean contested) {
    this.dominantTeam = dominantTeam;
    super.dominate(dominantTeam, dominantTime, contested);
  }
}

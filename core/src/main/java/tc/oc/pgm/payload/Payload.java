package tc.oc.pgm.payload;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;

import java.time.Duration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.material.Wool;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.payload.track.Track;
import tc.oc.pgm.regions.EmptyRegion;

public class Payload extends ControlPoint {

  private static final int PARTICLE_AMOUNT = 6; // 6 particles
  private static final int PARTICLE_ROTATION = 20; // ticks per full rotation

  private static final String METADATA_KEY = "payload";

  private static final Vector MINECART_OFFSET = new Vector(0, 0.0625, 0);
  private static final double MAX_OFFSET_SQ = Math.pow(0.25, 2); // Max offset of 0.25

  private final Match match;
  private final PayloadDefinition definition;

  private final Track track;
  private final Minecart minecart;
  private final PayloadRegion captureRegion;

  private Competitor dominantTeam;
  private Vector position;

  public Payload(Match match, PayloadDefinition definition) {
    super(match, definition);
    this.match = match;
    this.definition = definition;

    this.position = definition.getLocation();

    this.track = new Track(match, definition.getLocation());
    this.minecart = spawnMinecart();
    this.captureRegion = new PayloadRegion(this::getCenterPoint, definition.getRadius());

    this.playerTracker.setRegion(captureRegion);
  }

  private Minecart spawnMinecart() {
    Minecart minecart =
        match.getWorld().spawn(position.toLocation(match.getWorld()), Minecart.class);

    minecart.setDisplayBlock(new Wool(DyeColor.WHITE));
    minecart.setSlowWhenEmpty(true);
    minecart.setMetadata(METADATA_KEY, new FixedMetadataValue(PGM.get(), true));
    return minecart;
  }

  @Override
  public Region getCaptureRegion() {
    return captureRegion == null ? EmptyRegion.INSTANCE : captureRegion;
  }

  public Vector getCenterPoint() {
    return position;
  }

  @Override
  public void tick(Duration duration) {
    super.tick(duration);

    double progress = getCompletion();
    if (isCompleted()) progress = 1 - progress;

    Vector oldPos = position;
    position = track.getVector(progress);

    if (!oldPos.toBlockVector().equals(position.toBlockVector()))
      playerTracker.setRegion(captureRegion);

    tickParticles(match.getTick().tick);
    tickMinecart();
  }

  private Competitor getDisplayTeam() {
    return dominantTeam != null ? dominantTeam : controllingTeam != null ? controllingTeam : null;
  }

  private void tickParticles(long tick) {
    if (!definition.getDisplayFilter().query(match).isAllowed()) return;
    Competitor display = getDisplayTeam();
    Color color = display != null ? display.getFullColor() : Color.WHITE;

    double diff = Math.PI * 2 / PARTICLE_AMOUNT;
    double offset = (double) tick * diff / PARTICLE_ROTATION;

    Location loc = new Location(match.getWorld(), 0, 0, 0);
    for (int i = 0; i < PARTICLE_AMOUNT; i++) {
      double angle = i * diff + offset;
      // Height between 0.2 and 0.8
      double height = 0.5d + 0.3d * Math.sin(0.1d * tick + 0.5d * i);

      loc.setX(definition.getRadius() * Math.cos(angle));
      loc.setY(height);
      loc.setZ(definition.getRadius() * Math.sin(angle));
      loc.add(position);
      EFFECTS.coloredDust(match.getWorld(), loc, color);
    }

    if (definition.showBeam()) {
      DyeColor dyeColor = display != null ? display.getDyeColor() : DyeColor.WHITE;
      EFFECTS.beam(match.getWorld(), position.toLocation(match.getWorld()), dyeColor);
    }
  }

  private void tickMinecart() {
    Vector current = minecart.getLocation().toVector();
    Vector desired = position.clone().add(MINECART_OFFSET);

    if (current.distanceSquared(desired) > MAX_OFFSET_SQ)
      minecart.teleport(desired.toLocation(minecart.getWorld()));
    else minecart.setVelocity(desired.subtract(current));
  }

  private void updateWool() {
    Competitor display = getDisplayTeam();
    DyeColor color = display != null ? display.getDyeColor() : DyeColor.WHITE;

    Wool data = (Wool) minecart.getDisplayBlock();
    data.setColor(color);
    minecart.setDisplayBlock(data);
  }

  @Override
  protected void dominate(Competitor dominantTeam, Duration dominantTime, boolean contested) {
    if (this.dominantTeam != dominantTeam) {
      this.dominantTeam = dominantTeam;
      updateWool();
    }
    super.dominate(dominantTeam, dominantTime, contested);
  }

  public static boolean isPayload(Entity entity) {
    return entity.hasMetadata(METADATA_KEY);
  }
}

package tc.oc.pgm.flag.post;

import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.points.AngleProvider;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderLocation;
import tc.oc.pgm.teams.TeamFactory;

public class SinglePost extends PostDefinition {

  private static final int MAX_SPAWN_ATTEMPTS = 100;

  // The post's name, shown in chat when the flag is respawning
  private final @Nullable String postName;
  // Team that owns the post, affects various things
  private final @Nullable FeatureReference<TeamFactory> owner;
  // Time between a flag dropping and being recovered, can be infinite
  private final Duration recoverTime;
  // Fixed time between a flag being recovered and respawning at the post
  private final @Nullable Duration respawnTime;
  // Makes respawn time proportional to distance, flag "moves" back at this m/s
  private final @Nullable Double respawnSpeed;
  // Spawn points for the flag
  private final ImmutableList<PointProvider> returnPoints;
  // Search for spawn points sequentially, see equivalent field in SpawnInfo
  private final boolean sequential;
  // Flag enters Completed state when at this post
  private final boolean permanent;
  // Points awarded while any flag is at this post
  private final double pointsPerSecond;
  // Filter players who can pick up a flag at this post
  private final Filter pickupFilter;
  // Filter if a flag can respawn to this post
  private final Filter respawnFilter;

  public SinglePost(
      @Nullable String id,
      @Nullable String name,
      @Nullable FeatureReference<TeamFactory> owner,
      Duration recoverTime,
      @Nullable Duration respawnTime,
      @Nullable Double respawnSpeed,
      ImmutableList<PointProvider> returnPoints,
      boolean sequential,
      boolean permanent,
      double pointsPerSecond,
      Filter pickupFilter,
      Filter respawnFilter) {
    super(id);

    assertTrue(respawnTime == null || respawnSpeed == null);
    if (respawnSpeed != null) assertTrue(respawnSpeed > 0);

    this.owner = owner;
    this.recoverTime = recoverTime;
    this.respawnTime = respawnTime;
    this.respawnSpeed = respawnSpeed;
    this.returnPoints = returnPoints;
    this.sequential = sequential;
    this.permanent = permanent;
    this.pointsPerSecond = pointsPerSecond;
    this.pickupFilter = pickupFilter;
    this.respawnFilter = respawnFilter;
    this.postName = name;
  }

  @Override
  public SinglePost getFallback() {
    return this;
  }

  @Override
  public PostResolver createResolver(Match match) {
    return () -> this;
  }

  public @Nullable String getPostName() {
    return this.postName;
  }

  public @Nullable TeamFactory getOwner() {
    return this.owner == null ? null : this.owner.get();
  }

  public ChatColor getColor() {
    return this.owner == null ? ChatColor.WHITE : this.owner.get().getDefaultColor();
  }

  public Duration getRecoverTime() {
    return this.recoverTime;
  }

  public Duration getRespawnTime(double distance) {
    if (respawnTime != null) {
      return respawnTime;
    } else if (respawnSpeed != null) {
      return Duration.ofSeconds(Math.round(distance / respawnSpeed));
    } else {
      return Duration.ZERO;
    }
  }

  public ImmutableList<PointProvider> getReturnPoints() {
    return this.returnPoints;
  }

  public boolean isSequential() {
    return this.sequential;
  }

  public Boolean isPermanent() {
    return this.permanent;
  }

  public double getPointsPerSecond() {
    return this.pointsPerSecond;
  }

  public Filter getPickupFilter() {
    return this.pickupFilter;
  }

  public Filter getRespawnFilter() {
    return respawnFilter;
  }

  public Location getReturnPoint(Flag flag, AngleProvider yawProvider) {
    Location location = getReturnPoint(flag);
    if (location instanceof PointProviderLocation && !((PointProviderLocation) location).hasYaw()) {
      location.setYaw(yawProvider.getAngle(location.toVector()));
    }
    return location;
  }

  private Location getReturnPoint(Flag flag) {
    if (this.sequential) {
      for (PointProvider provider : this.returnPoints) {
        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
          Location loc = roundToBlock(provider.getPoint(flag.getMatch(), null));
          if (flag.canDropAt(loc)) {
            return loc;
          }
        }
      }

      // could not find a good spot, fallback to the last provider
      return this.returnPoints.get(this.returnPoints.size() - 1).getPoint(flag.getMatch(), null);

    } else {
      Random random = new Random();
      for (int i = 0; i < MAX_SPAWN_ATTEMPTS * this.returnPoints.size(); i++) {
        PointProvider provider = this.returnPoints.get(random.nextInt(this.returnPoints.size()));
        Location loc = roundToBlock(provider.getPoint(flag.getMatch(), null));
        if (flag.canDropAt(loc)) {
          return loc;
        }
      }

      // could not find a good spot, settle for any spot
      PointProvider provider = this.returnPoints.get(random.nextInt(this.returnPoints.size()));
      return this.returnPoints
          .get(random.nextInt(this.returnPoints.size()))
          .getPoint(flag.getMatch(), null);
    }
  }

  private Location roundToBlock(Location loc) {
    Location newLoc = loc.clone();

    newLoc.setX(Math.floor(loc.getX()) + 0.5);
    newLoc.setY(Math.floor(loc.getY()));
    newLoc.setZ(Math.floor(loc.getZ()) + 0.5);

    return newLoc;
  }
}

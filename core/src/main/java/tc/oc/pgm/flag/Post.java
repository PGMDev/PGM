package tc.oc.pgm.flag;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.Random;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.points.AngleProvider;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderLocation;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "post")
public class Post extends SelfIdentifyingFeatureDefinition {
  public static final Duration DEFAULT_RETURN_TIME = Duration.ofSeconds(30);
  public static final double DEFAULT_RESPAWN_SPEED = 8;

  private static final int MAX_SPAWN_ATTEMPTS = 100;

  private final @Nullable FeatureReference<TeamFactory>
      owner; // Team that owns the post, affects various things
  private final Duration
      recoverTime; // Time between a flag dropping and being recovered, can be infinite
  private final @Nullable Duration
      respawnTime; // Fixed time between a flag being recovered and respawning at the post
  private final @Nullable Double
      respawnSpeed; // Makes respawn time proportional to distance, flag "moves" back at this m/s
  private final ImmutableList<PointProvider> returnPoints; // Spawn points for the flag
  private final boolean
      sequential; // Search for spawn points sequentially, see equivalent field in SpawnInfo
  private final boolean permanent; // Flag enters Completed state when at this post
  private final double pointsPerSecond; // Points awarded while any flag is at this post
  private final Filter pickupFilter; // Filter players who can pickup a flag at this post
  private final @Nullable String
      postName; // The name of the post to be shown in chat when the flag is respawning

  private boolean specifiedPost = false;

  public Post(
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
      Filter pickupFilter) {

    super(id);
    checkArgument(respawnTime == null || respawnSpeed == null);
    if (respawnSpeed != null) checkArgument(respawnSpeed > 0);

    this.owner = owner;
    this.recoverTime = recoverTime;
    this.respawnTime = respawnTime;
    this.respawnSpeed = respawnSpeed;
    this.returnPoints = returnPoints;
    this.sequential = sequential;
    this.permanent = permanent;
    this.pointsPerSecond = pointsPerSecond;
    this.pickupFilter = pickupFilter;
    this.postName = name;
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

  public Location getReturnPoint(Flag flag, AngleProvider yawProvider) {
    Location location = getReturnPoint(flag);
    if (location instanceof PointProviderLocation && !((PointProviderLocation) location).hasYaw()) {
      location.setYaw(yawProvider.getAngle(location.toVector()));
    }
    return location;
  }

  public boolean isSpecifiedPost() {
    return this.specifiedPost;
  }

  public void setSpecifiedPost(boolean value) {
    this.specifiedPost = value;
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

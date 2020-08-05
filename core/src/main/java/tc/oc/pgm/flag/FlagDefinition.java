package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.DyeColor;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.bukkit.BukkitUtils;

@FeatureInfo(name = "flag")
public class FlagDefinition extends ProximityGoalDefinition {

  private static String makeName(@Nullable String name, @Nullable DyeColor color) {
    if (name != null) return name;
    if (color != null)
      return color.name().charAt(0) + color.name().substring(1).toLowerCase() + " Flag";
    return "Flag";
  }

  private final @Nullable DyeColor
      color; // Flag color, null detects color from the banner at match load time
  private final Post defaultPost; // Flag starts the match at this post
  private final ImmutableList<Post> posts;
  private final @Nullable FeatureReference<TeamFactory>
      owner; // Team that owns the flag, affects various things
  private final double
      pointsPerCapture; // Points awarded for capturing this flag, in addition to points from the
  // Net
  private final double pointsPerSecond; // Points awarded while carrying this flag
  private final Filter pickupFilter; // Filter players who can pickup this flag
  private final Filter captureFilter; // Filter players who can capture this flag
  private final @Nullable Kit pickupKit; // Kit to give on flag pickup
  private final @Nullable Kit dropKit; // Kit to give carrier when they drop the flag
  private final @Nullable Kit carryKit; // Kit to give to/take from the flag carrier
  private final boolean multiCarrier; // Affects how the flag appears in the scoreboard
  private final @Nullable Component carryMessage; // Custom message to show flag carrier
  private final boolean dropOnWater; // Flag can freeze water to drop on it
  private final boolean showBeam;
  private final boolean sequential;
  private boolean
      showRespawnOnPickup; // When a flag is picked up, if true, it will display where it will
  // respawn. Will be set to false if a net defines a different respawn post.

  public FlagDefinition(
      @Nullable String id,
      @Nullable String name,
      @Nullable Boolean required,
      boolean visible,
      @Nullable DyeColor color,
      Post defaultPost,
      ImmutableList<Post> posts,
      @Nullable FeatureReference<TeamFactory> owner,
      double pointsPerCapture,
      double pointsPerSecond,
      Filter pickupFilter,
      Filter captureFilter,
      @Nullable Kit pickupKit,
      @Nullable Kit dropKit,
      @Nullable Kit carryKit,
      boolean multiCarrier,
      @Nullable Component carryMessage,
      boolean dropOnWater,
      boolean showBeam,
      @Nullable ProximityMetric flagProximityMetric,
      @Nullable ProximityMetric netProximityMetric,
      boolean sequential,
      boolean showRespawnOnPickup) {

    // We can't use the owner field in OwnedGoal because our owner
    // is a reference that can't be resolved until after parsing.
    super(
        id,
        makeName(name, color),
        required,
        visible,
        null,
        flagProximityMetric,
        netProximityMetric);

    this.color = color;
    this.defaultPost = defaultPost;
    this.posts = posts;
    this.owner = owner;
    this.pointsPerCapture = pointsPerCapture;
    this.pointsPerSecond = pointsPerSecond;
    this.pickupFilter = pickupFilter;
    this.captureFilter = captureFilter;
    this.pickupKit = pickupKit;
    this.dropKit = dropKit;
    this.carryKit = carryKit;
    this.multiCarrier = multiCarrier;
    this.carryMessage = carryMessage;
    this.dropOnWater = dropOnWater;
    this.showBeam = showBeam;
    this.sequential = sequential;
    this.showRespawnOnPickup = showRespawnOnPickup;
  }

  public @Nullable DyeColor getColor() {
    return this.color;
  }

  @Override
  public String getColoredName() {
    if (this.getColor() != null) {
      return BukkitUtils.dyeColorToChatColor(this.getColor()) + this.getName();
    } else {
      return super.getColoredName();
    }
  }

  public Post getDefaultPost() {
    return this.defaultPost;
  }

  public ImmutableList<Post> getPosts() {
    return this.posts;
  }

  @Override
  public @Nullable TeamFactory getOwner() {
    return this.owner == null ? null : this.owner.get();
  }

  // Override the default ID to not rely on owner, which is
  // nullable and not yet resolved when the ID is needed.
  @Override
  protected String getDefaultId() {
    return makeDefaultId() + "--" + makeId(getName());
  }

  public double getPointsPerCapture() {
    return this.pointsPerCapture;
  }

  public double getPointsPerSecond() {
    return this.pointsPerSecond;
  }

  public Filter getPickupFilter() {
    return this.pickupFilter;
  }

  public Filter getCaptureFilter() {
    return captureFilter;
  }

  public @Nullable Kit getPickupKit() {
    return pickupKit;
  }

  public @Nullable Kit getDropKit() {
    return dropKit;
  }

  public @Nullable Kit getCarryKit() {
    return carryKit;
  }

  public boolean hasMultipleCarriers() {
    return multiCarrier;
  }

  public @Nullable Component getCarryMessage() {
    return carryMessage;
  }

  public boolean canDropOnWater() {
    return dropOnWater;
  }

  public boolean showBeam() {
    return showBeam;
  }

  public boolean isSequential() {
    return sequential;
  }

  public boolean willShowRespawnOnPickup() {
    return showRespawnOnPickup;
  }

  public void setShowRespawnOnPickup(boolean value) {
    this.showRespawnOnPickup = value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Flag getGoal(Match match) {
    return (Flag) super.getGoal(match);
  }

  public boolean canPickup(Query query) {
    return getPickupFilter().query(query).isAllowed()
        && getDefaultPost().getPickupFilter().query(query).isAllowed();
  }

  public boolean canCapture(Query query, Collection<Net> nets) {
    if (getCaptureFilter().query(query).isDenied()) return false;
    for (Net net : nets) {
      if (net.getCaptureFilter().query(query).isAllowed()) return true;
    }
    return false;
  }
}

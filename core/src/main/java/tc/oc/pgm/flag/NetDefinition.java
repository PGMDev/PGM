package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableSet;
import net.kyori.adventure.text.Component;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.teams.TeamFactory;

@FeatureInfo(name = "net")
public class NetDefinition extends SelfIdentifyingFeatureDefinition {

  private final Region region; // Region flag carrier must enter to capture
  private final Filter captureFilter; // Carrier must pass this filter to capture
  private final Filter respawnFilter; // Captured flags will not respawn until they pass this filter
  private final @Nullable FeatureReference<TeamFactory>
      owner; // Team that gets points for captures in this net, null to give points to flag carrier
  private final double pointsPerCapture; // Points awarded per capture
  private final boolean
      sticky; // If capture is delayed by filter, carrier does not have to stay inside the net
  private final @Nullable Component
      denyMessage; // Message to show carrier when capture is prevented by filter
  private final @Nullable Component
      respawnMessage; // Message to broadcast when respawn is prevented by filter or respawnTogether
  private final @Nullable PostDefinition
      returnPost; // Post to send flags after capture, null to send to their current post
  private final ImmutableSet<FlagDefinition>
      capturableFlags; // Flags that can be captured in this net
  private final ImmutableSet<FlagDefinition>
      recoverableFlags; // Flags that are force returned on capture, aside from the flag being
  // captured
  private final boolean respawnTogether; // Delay respawn until all capturableFlags are captured
  private @Nullable Vector proximityLocation;

  public NetDefinition(
      @Nullable String id,
      Region region,
      Filter captureFilter,
      Filter respawnFilter,
      @Nullable FeatureReference<TeamFactory> owner,
      double pointsPerCapture,
      boolean sticky,
      @Nullable Component denyMessage,
      @Nullable Component respawnMessage,
      @Nullable PostDefinition returnPost,
      ImmutableSet<FlagDefinition> capturableFlags,
      ImmutableSet<FlagDefinition> recoverableFlags,
      boolean respawnTogether,
      @Nullable Vector proximityLocation) {

    super(id);
    this.region = region;
    this.captureFilter = captureFilter;
    this.respawnFilter = respawnFilter;
    this.owner = owner;
    this.pointsPerCapture = pointsPerCapture;
    this.sticky = sticky;
    this.denyMessage = denyMessage;
    this.respawnMessage = respawnMessage;
    this.returnPost = returnPost;
    this.capturableFlags = capturableFlags;
    this.recoverableFlags = recoverableFlags;
    this.respawnTogether = respawnTogether;
    this.proximityLocation = proximityLocation;
  }

  public Region getRegion() {
    return this.region;
  }

  public Filter getCaptureFilter() {
    return captureFilter;
  }

  public Filter getRespawnFilter() {
    return respawnFilter;
  }

  public @Nullable Component getRespawnMessage() {
    return respawnMessage;
  }

  public @Nullable TeamFactory getOwner() {
    return this.owner == null ? null : this.owner.get();
  }

  public double getPointsPerCapture() {
    return pointsPerCapture;
  }

  public boolean isSticky() {
    return sticky;
  }

  public @Nullable Component getDenyMessage() {
    return denyMessage;
  }

  public @Nullable PostDefinition getReturnPost() {
    return this.returnPost;
  }

  public ImmutableSet<FlagDefinition> getCapturableFlags() {
    return capturableFlags;
  }

  public ImmutableSet<FlagDefinition> getRecoverableFlags() {
    return recoverableFlags;
  }

  public boolean isRespawnTogether() {
    return respawnTogether;
  }

  public Vector getProximityLocation() {
    if (proximityLocation == null) {
      proximityLocation = getRegion().getBounds().getCenterPoint();
    }
    return proximityLocation;
  }
}

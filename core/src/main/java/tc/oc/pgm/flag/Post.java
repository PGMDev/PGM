package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.flag.post.PostResolver;
import tc.oc.pgm.flag.post.SinglePost;
import tc.oc.pgm.points.AngleProvider;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.teams.TeamFactory;

public class Post implements Feature<PostDefinition> {
  private final Match match;
  private final PostDefinition definition;
  private final PostResolver singleDefinition;

  public Post(Match match, PostDefinition definition) {
    this.match = match;
    this.definition = definition;
    this.singleDefinition = definition.createResolver(match);
  }

  @Override
  public String getId() {
    return getDefinition().getId();
  }

  @Override
  public PostDefinition getDefinition() {
    return definition;
  }

  public SinglePost peekNext(Flag flag) {
    return singleDefinition.peekNext(flag);
  }

  public SinglePost getNext(Flag flag) {
    return singleDefinition.getNext(flag);
  }

  public SinglePost getCurrent() {
    return singleDefinition.get();
  }

  public @Nullable String getPostName() {
    return getCurrent().getPostName();
  }

  public @Nullable TeamFactory getOwner() {
    return getCurrent().getOwner();
  }

  public ChatColor getColor() {
    return getCurrent().getColor();
  }

  public Duration getRecoverTime() {
    return getCurrent().getRecoverTime();
  }

  public Duration getRespawnTime(double distance) {
    return getCurrent().getRespawnTime(distance);
  }

  public ImmutableList<PointProvider> getReturnPoints() {
    return getCurrent().getReturnPoints();
  }

  public boolean isSequential() {
    return getCurrent().isSequential();
  }

  public Boolean isPermanent() {
    return getCurrent().isPermanent();
  }

  public double getPointsPerSecond() {
    return getCurrent().getPointsPerSecond();
  }

  public Filter getPickupFilter() {
    return getCurrent().getPickupFilter();
  }

  public Location getReturnPoint(Flag flag, AngleProvider yawProvider) {
    return getNext(flag).getReturnPoint(flag, yawProvider);
  }
}

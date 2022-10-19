package tc.oc.pgm.flag.post;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "post")
public abstract class PostDefinition extends SelfIdentifyingFeatureDefinition {
  public static final Duration DEFAULT_RETURN_TIME = Duration.ofSeconds(30);
  public static final double DEFAULT_RESPAWN_SPEED = 8;

  public PostDefinition(String id) {
    super(id);
  }

  @Override
  protected String getDefaultId() {
    return super.makeDefaultId();
  }

  public abstract PostResolver createResolver(Match match);

  public abstract SinglePost getFallback();

  public static String makeDefaultId(@Nullable String name, AtomicInteger serial) {
    return "--"
        + makeTypeName(PostDefinition.class)
        + "-"
        + (name != null ? makeId(name) : serial.getAndIncrement());
  }
}

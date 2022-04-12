package tc.oc.pgm.flag.post;

import java.time.Duration;
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

  public abstract PostResolver createResolver(Match match);

  public abstract SinglePost getFallback();
}

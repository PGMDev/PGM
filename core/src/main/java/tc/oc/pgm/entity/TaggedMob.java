package tc.oc.pgm.entity;

import org.bukkit.entity.LivingEntity;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.match.Match;

public interface TaggedMob extends FeatureDefinition {
  String getId();

  LivingEntity spawn(Match match, double x, double y, double z, float pitch, float yaw);
}

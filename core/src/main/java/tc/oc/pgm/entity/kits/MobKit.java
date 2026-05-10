package tc.oc.pgm.entity.kits;

import org.bukkit.entity.LivingEntity;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.util.xml.InvalidXMLException;

@FeatureInfo(name = "mob-kit")
public interface MobKit extends FeatureDefinition {
  void apply(LivingEntity entity);

  default void validate(Class<? extends LivingEntity> mobType, Element source)
      throws InvalidXMLException {}
}

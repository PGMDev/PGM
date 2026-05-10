package tc.oc.pgm.entity.kits;

import org.bukkit.entity.LivingEntity;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class XMLMobKitReference extends XMLFeatureReference<MobKit> implements MobKit {

  public XMLMobKitReference(
      FeatureDefinitionContext context, Node node, @Nullable String id, Class<MobKit> type) {
    super(context, node, id, type);
  }

  @Override
  public void apply(LivingEntity entity) {
    get().apply(entity);
  }

  @Override
  public void validate(Class<? extends LivingEntity> mobType, Element source)
      throws InvalidXMLException {
    get().validate(mobType, source);
  }
}

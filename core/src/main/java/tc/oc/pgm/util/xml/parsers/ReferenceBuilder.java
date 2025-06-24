package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.util.xml.Node;

public class ReferenceBuilder<T extends FeatureDefinition>
    extends Builder<FeatureReference<T>, ReferenceBuilder<T>> {
  private final FeatureDefinitionContext features;
  private final Class<T> type;

  public ReferenceBuilder(
      FeatureDefinitionContext features, Class<T> type, Element el, String... prop) {
    super(el, prop);
    this.features = features;
    this.type = type;
  }

  @Override
  protected FeatureReference<T> parse(Node node) {
    return features.createReference(node, type);
  }

  public ReferenceBuilder<T> validate(FeatureValidation<T> fv) {
    super.validate((ref, node) -> features.validate(ref, fv));
    return this;
  }

  @Override
  protected ReferenceBuilder<T> getThis() {
    return this;
  }
}

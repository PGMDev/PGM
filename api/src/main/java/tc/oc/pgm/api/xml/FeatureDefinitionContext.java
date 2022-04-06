package tc.oc.pgm.api.xml;

import java.util.Collection;
import javax.annotation.Nullable;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.feature.FeatureValidation;

public interface FeatureDefinitionContext {
  FeatureDefinition get(String id);

  <T extends FeatureDefinition> T get(String id, Class<T> type);

  <T extends FeatureDefinition> Iterable<T> getAll(Class<T> type);

  Element getNode(FeatureDefinition definition);

  void addFeature(@Nullable Element node, @Nullable String id, FeatureDefinition definition)
      throws InvalidXMLException;

  void addFeature(@Nullable Element node, FeatureDefinition definition) throws InvalidXMLException;

  <T extends XMLFeatureReference<?>> T addReference(T reference);

  <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, String id, Class<T> type);

  <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, String id, Class<T> type, XMLFeatureReference<T> def);

  <T extends FeatureDefinition> XMLFeatureReference<T> createReference(Node node, Class<T> type);

  <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, Class<T> type, XMLFeatureReference<T> def);

  <T extends FeatureDefinition> void validate(
      FeatureReference<T> reference, FeatureValidation<T> validation) throws InvalidXMLException;

  Collection<InvalidXMLException> resolveReferences();
}

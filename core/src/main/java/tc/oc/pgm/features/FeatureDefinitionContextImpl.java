package tc.oc.pgm.features;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import java.util.*;
import javax.annotation.Nullable;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.feature.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.api.xml.FeatureDefinitionContext;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;
import tc.oc.pgm.api.xml.XMLFeatureReference;

public class FeatureDefinitionContextImpl implements FeatureDefinitionContext {

  private final Set<FeatureDefinition> definitions = new HashSet<>();
  private final Map<String, FeatureDefinition> byId = new HashMap<>();
  private final Map<FeatureDefinition, Element> definitionNodes = new HashMap<>();
  private final List<XMLFeatureReference<?>> references = new ArrayList<>();
  private final SetMultimap<FeatureReference<?>, FeatureValidation<?>> validations =
      HashMultimap.create();

  @Override
  public FeatureDefinition get(String id) {
    return byId.get(id);
  }

  /**
   * Return a feature with the given ID and type, or null if no such feature exists. If the ID
   * exists but is the wrong type, this method will still return null.
   */
  @Override
  public <T extends FeatureDefinition> T get(String id, Class<T> type) {
    FeatureDefinition definition = get(id);
    return type.isInstance(definition) ? type.cast(definition) : null;
  }

  @Override
  public <T extends FeatureDefinition> Iterable<T> getAll(Class<T> type) {
    return Iterables.filter(definitions, type);
  }

  /** Return the XML element associated with the given feature */
  @Override
  public Element getNode(FeatureDefinition definition) {
    return definitionNodes.get(definition);
  }

  /**
   * Add the given feature to the context with an optional ID. If no ID is given, there is no way to
   * retrieve the feature from the context. However, it can still be passed to {@link #getNode} to
   * retrieve the element passed here.
   */
  @Override
  public void addFeature(@Nullable Element node, @Nullable String id, FeatureDefinition definition)
      throws InvalidXMLException {
    if (definitions.add(definition)) {
      if (id != null) {
        FeatureDefinition old = byId.put(id, definition);
        if (old != null && old != definition) {
          byId.put(id, old);
          throw new InvalidXMLException(
              "The ID '" + id + "' is already in use by a different feature", node);
        }
      }

      if (node != null) {
        definitionNodes.put(definition, node);
      }
    }
  }

  /**
   * Add the given feature to the context, parsing its ID from the "id" attribute of the given
   * element, if present.
   */
  @Override
  public void addFeature(@Nullable Element node, FeatureDefinition definition)
      throws InvalidXMLException {
    addFeature(node, node.getAttributeValue("id"), definition);
  }

  /**
   * Add the given self-identifying feature to the context, under the ID returned from its getId
   * method.
   */
  public void addFeature(@Nullable Element node, SelfIdentifyingFeatureDefinition definition)
      throws InvalidXMLException {
    addFeature(node, definition.getId(), definition);
  }

  /**
   * Add an {@link XMLFeatureReference} to the internal list of references which are resolved after
   * all modules have loaded.
   */
  @Override
  public <T extends XMLFeatureReference<?>> T addReference(T reference) {
    references.add(reference);
    return reference;
  }

  /**
   * Create and return an {@link XMLFeatureReference} after adding it to the internal resolve list
   * by calling {@link #addReference}
   *
   * @param node The XML node containing the ID of the feature
   * @param type The type of feature
   * @return A reference to the feature
   */
  @Override
  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, String id, Class<T> type) {
    return addReference(new XMLFeatureReference<>(this, node, id, type));
  }

  @Override
  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, String id, Class<T> type, XMLFeatureReference<T> def) {
    return node == null ? def : createReference(node, id, type);
  }

  @Override
  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, Class<T> type) {
    return this.createReference(node, null, type);
  }

  @Override
  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, Class<T> type, XMLFeatureReference<T> def) {
    return this.createReference(node, null, type, def);
  }

  /** Enque a validation to run on the referenced feature eventually */
  @Override
  public <T extends FeatureDefinition> void validate(
      FeatureReference<T> reference, FeatureValidation<T> validation) throws InvalidXMLException {
    if (reference.isResolved()) {
      validation.validate(reference.get(), reference.getNode());
    } else {
      validations.put(reference, validation);
    }
  }

  @Override
  public Collection<InvalidXMLException> resolveReferences() {
    List<InvalidXMLException> errors = new ArrayList<>();
    for (XMLFeatureReference<?> reference : references) {
      try {
        reference.resolve();
        for (FeatureValidation validation : validations.get(reference)) {
          validation.validate(reference.get(), reference.getNode());
        }
      } catch (InvalidXMLException e) {
        errors.add(e);
      }
    }
    return errors;
  }
}

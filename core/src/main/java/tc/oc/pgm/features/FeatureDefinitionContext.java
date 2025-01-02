package tc.oc.pgm.features;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.util.collection.ContextStore;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class FeatureDefinitionContext extends ContextStore<FeatureDefinition> {

  private final Set<FeatureDefinition> definitions = new HashSet<>();
  private final Map<FeatureDefinition, Element> definitionNodes = new HashMap<>();
  private final List<XMLFeatureReference<?>> references = new ArrayList<>();

  private final List<PendingValidation<?>> validations = new ArrayList<>();

  public static String parseId(Element node) {
    return node == null ? null : node.getAttributeValue("id");
  }

  /** Return the XML element associated with the given feature */
  public Element getNode(FeatureDefinition definition) {
    return definitionNodes.get(definition);
  }

  @Override
  public String add(FeatureDefinition obj) {
    String id = super.add(obj);
    definitions.add(obj);
    return id;
  }

  @Override
  public void add(String name, FeatureDefinition obj) {
    super.add(name, obj);
    this.definitions.add(obj);
  }

  /**
   * Add the given feature to the context with an optional ID. If no ID is given, there is no way to
   * retrieve the feature from the context. However, it can still be passed to {@link #getNode} to
   * retrieve the element passed here.
   */
  public void addFeature(@Nullable Element node, @Nullable String id, FeatureDefinition definition)
      throws InvalidXMLException {
    definitions.add(definition);
    if (id != null) {
      FeatureDefinition old = this.store.put(id, definition);
      if (old != null && old != definition) {
        this.store.put(id, old);
        throw new InvalidXMLException(
            "The ID '" + id + "' is already in use by a different feature", node);
      }
    }

    if (node != null) {
      definitionNodes.put(definition, node);
    }
  }

  /**
   * Add the given feature to the context, parsing its ID from the "id" attribute of the given
   * element, if present.
   */
  public void addFeature(@Nullable Element node, FeatureDefinition definition)
      throws InvalidXMLException {
    addFeature(node, parseId(node), definition);
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
  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, String id, Class<T> type) {
    return addReference(new XMLFeatureReference<>(this, node, id, type));
  }

  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, String id, Class<T> type, XMLFeatureReference<T> def) {
    return node == null ? def : createReference(node, id, type);
  }

  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, Class<T> type) {
    return this.createReference(node, null, type);
  }

  public <T extends FeatureDefinition> XMLFeatureReference<T> createReference(
      Node node, Class<T> type, XMLFeatureReference<T> def) {
    return this.createReference(node, null, type, def);
  }

  /**
   * Warning: this should only be used when you're certain those features load before you call this
   *
   * @param node The XML node containing the ID of the feature
   * @param cls The type of feature
   * @return The feature if available
   * @throws InvalidXMLException If the requested feature is not available
   */
  public <T extends FeatureDefinition> T resolve(Node node, Class<T> cls)
      throws InvalidXMLException {
    return resolve(node, node.getValueNormalize(), cls);
  }

  /**
   * Warning: this should only be used when you're certain those features load before you call this
   *
   * @param node The XML node containing the ID of the feature
   * @param cls The type of feature
   * @return The feature if available
   * @throws InvalidXMLException If the requested feature is not available
   */
  public <T extends FeatureDefinition> T resolve(Node node, String id, Class<T> cls)
      throws InvalidXMLException {
    T val = get(id, cls);
    if (val == null)
      throw new InvalidXMLException(
          "Unknown " + SelfIdentifyingFeatureDefinition.makeTypeName(cls) + " ID '" + id + "'",
          node);
    return val;
  }

  public <T extends FeatureDefinition> void validate(
      T definition, FeatureValidation<T> validation, Node node) throws InvalidXMLException {
    validations.add(new PendingValidation<>(definition, validation, node));
  }

  public <T extends FeatureDefinition> void validate(
      FeatureReference<T> reference, FeatureValidation<T> validation) throws InvalidXMLException {
    validations.add(new PendingValidation<>(reference, validation, reference.getNode()));
  }

  public Collection<InvalidXMLException> resolveReferences() {
    List<InvalidXMLException> errors = new ArrayList<>();
    for (XMLFeatureReference<?> reference : references) {
      try {
        reference.resolve();
      } catch (InvalidXMLException e) {
        errors.add(e);
      }
    }
    for (PendingValidation<?> validation : validations) {
      try {
        validation.validate();
      } catch (InvalidXMLException e) {
        errors.add(e);
      }
    }
    return errors;
  }

  @Override
  public Collection<FeatureDefinition> getAll() {
    return this.definitions;
  }

  @Override
  public <T extends FeatureDefinition> Iterable<T> getAll(Class<T> type) {
    return Iterables.filter(definitions, type);
  }

  private static class PendingValidation<T extends FeatureDefinition> {
    private final T definition;
    private final FeatureReference<T> reference;
    private final FeatureValidation<T> validation;
    private final Node node;

    public PendingValidation(T definition, FeatureValidation<T> validation, Node node) {
      this.definition = definition;
      this.reference = null;
      this.validation = validation;
      this.node = node;
    }

    private PendingValidation(
        FeatureReference<T> reference, FeatureValidation<T> validation, Node node) {
      this.definition = null;
      this.reference = reference;
      this.validation = validation;
      this.node = node;
    }

    public void validate() throws InvalidXMLException {
      if (definition != null) validation.validate(definition, node);
      if (reference != null) {
        if (!reference.isResolved()) reference.resolve();
        validation.validate(reference.get(), node);
      }
    }
  }
}

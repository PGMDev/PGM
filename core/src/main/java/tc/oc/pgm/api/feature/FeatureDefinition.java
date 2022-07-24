package tc.oc.pgm.api.feature;

import java.util.stream.Stream;

/**
 * Used as a base-class for all templates of features. Kept around during parsing time so that we
 * can easily convert a definition into a Feature for match-time, and persist the ID from the XML.
 * Also stored in the {@link tc.oc.pgm.features.FeatureDefinitionContext} so that we don't collide
 * IDs.
 */
public interface FeatureDefinition {

  default FeatureDefinition get() {
    return this;
  }

  /** Concrete class of the definition object (rather than a proxy). */
  default Class<? extends FeatureDefinition> getDefinitionType() {
    return getClass();
  }

  default boolean isInstanceOf(Class<? extends FeatureDefinition> type) {
    return type.isAssignableFrom(getDefinitionType());
  }

  default Stream<? extends FeatureDefinition> dependencies() {
    return Stream.of();
  }

  default <T extends FeatureDefinition> Stream<? extends T> dependencies(Class<T> type) {
    //noinspection unchecked
    return (Stream<T>) dependencies().filter(type::isInstance);
  }

  default <T extends FeatureDefinition> Stream<? extends T> deepDependencies(Class<T> type) {
    Stream<? extends T> stream = dependencies(type).flatMap(dep -> dep.deepDependencies(type));
    if (isInstanceOf(type)) {
      //noinspection unchecked
      stream = Stream.concat(Stream.of((T) get()), stream);
    }
    return stream;
  }
}

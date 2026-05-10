package tc.oc.pgm.api.map.factory;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.module.ModuleContext;
import tc.oc.pgm.entity.kits.MobKitParser;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLFluentParser;

/** A factory for creating {@link MapInfo}s and {@link MapContext}s. */
public interface MapFactory extends ModuleContext<MapModule<?>>, AutoCloseable {

  /**
   * A parser able to parse most commonly required things, with a fluent interface.
   *
   * @return A {@link XMLFluentParser}
   */
  XMLFluentParser getParser();

  /**
   * Get the {@link RegionParser} for parsing region references.
   *
   * @return A {@link RegionParser}.
   */
  RegionParser getRegions();

  /**
   * Get the {@link FilterParser} for parsing filter references.
   *
   * @return A {@link FilterParser}.
   */
  FilterParser getFilters();

  /**
   * Get the {@link KitParser} for parsing filter references.
   *
   * @return A {@link KitParser}.
   */
  KitParser getKits();

  /**
   * Get the {@link MobKitParser} for parsing mob-kit definitions and references.
   *
   * @return A {@link MobKitParser}.
   */
  MobKitParser getMobKits();

  /**
   * Get the {@link FeatureDefinitionContext} for registering feature references.
   *
   * @return A {@link FeatureDefinitionContext}.
   */
  FeatureDefinitionContext getFeatures();

  /**
   * Get the effective {@link Version} for the map syntax.
   *
   * @see tc.oc.pgm.api.map.MapProtos
   * @return A {@link Version}.
   */
  Version getProto();

  /**
   * Load each {@link MapModule} and create a persistent context.
   *
   * @return A {@link MapContext}.
   * @throws MapException If there was an error loading the context.
   */
  MapContext load() throws MapException;

  /**
   * Logs an XML warning without a reference to a specific node. Prefer calling
   * {@link MapFactory#warn(String, Node)} instead.
   *
   * @param message The warning message
   */
  default void warn(@NonNull String message) {
    warn(message, (Node) null);
  }

  /**
   * Logs an XML warning, possibly related to the given node.
   *
   * @param message The warning message
   * @param node The node involved in the warning, if any
   */
  void warn(@NonNull String message, @Nullable Node node);

  /**
   * Logs an XML warning, possibly related to the given element.
   *
   * @param message The warning message
   * @param el The element involved in the warning, if any
   */
  default void warn(@NonNull String message, @Nullable Element el) {
    warn(message, Node.fromNullable(el));
  }

  /**
   * Logs an XML warning, possibly related to the given attribute.
   *
   * @param message The warning message
   * @param attr The attribute involved in the warning, if any
   */
  default void warn(@NonNull String message, @Nullable Attribute attr) {
    warn(message, Node.fromNullable(attr));
  }
}

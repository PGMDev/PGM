package tc.oc.pgm.api.map.factory;

import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.module.ModuleContext;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.Version;

/** A factory for creating {@link MapInfo}s and {@link MapContext}s. */
public interface MapFactory extends ModuleContext<MapModule>, AutoCloseable {

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
}

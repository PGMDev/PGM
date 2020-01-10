package tc.oc.pgm.api.map;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.module.ModuleContext;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionParser;

public interface MapContext extends ModuleContext<MapModule> {

  /**
   * Get the {@link Logger} associated with the map.
   *
   * @return A {@link Logger}.
   */
  Logger getLogger();

  /**
   * Get the {@link MapInfo} associated with the map.
   *
   * @return The {@link MapInfo} or {@code null} if not yet parsed.
   */
  MapInfo getInfo();

  /**
   * Get the {@link MapSource} where the source files can be downloaded.
   *
   * @return A {@link MapSource} or {@code null} if it was garbage collected.
   */
  @Nullable
  MapSource getSource();

  /**
   * Get whether {@link #load()} was successful and all {@link #getModules()} are present.
   *
   * @return True if loaded.
   */
  boolean isLoaded();

  /**
   * Loads or re-loads all of the {@link MapModule}s.
   *
   * @throws ModuleLoadException If an errors occurs while loading the modules.
   * @throws MapNotFoundException If the map files could not be found.
   */
  @Override
  void load() throws ModuleLoadException, MapNotFoundException;

  /**
   * Unloads any {@link MapModule}s and releases any references to any unnecessary parsing objects.
   *
   * <p>Should be able to be restored with a call to {@link #load()}.
   */
  @Override
  void unload();

  /**
   * Get legacy helpers and utilities for map parsing.
   *
   * <p>While this is necessary today, we hope to refactor them out in the future so they are not
   * encapsulated in {@link MapContext}.
   *
   * @return Legacy helpers, it's okay to use them.
   */
  @Deprecated
  Legacy legacy();

  interface Legacy {
    RegionParser getRegions();

    FilterParser getFilters();

    KitParser getKits();

    FeatureDefinitionContext getFeatures();
  }
}

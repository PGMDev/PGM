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
import tc.oc.util.Version;

/** A factory for creating {@link MapInfo}s and {@link MapContext}s. */
public interface MapFactory extends ModuleContext<MapModule>, AutoCloseable {

  RegionParser getRegions();

  FilterParser getFilters();

  KitParser getKits();

  FeatureDefinitionContext getFeatures();

  Version getProto();

  MapContext load() throws MapException;
}

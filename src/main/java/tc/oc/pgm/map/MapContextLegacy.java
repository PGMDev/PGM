package tc.oc.pgm.map;

import java.lang.ref.WeakReference;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FeatureFilterParser;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.LegacyFilterParser;
import tc.oc.pgm.kits.FeatureKitParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.kits.LegacyKitParser;
import tc.oc.pgm.regions.FeatureRegionParser;
import tc.oc.pgm.regions.LegacyRegionParser;
import tc.oc.pgm.regions.RegionParser;

public class MapContextLegacy implements MapContext.Legacy {

  private final WeakReference<MapContext> map;
  private Boolean legacy = null;
  private RegionParser regions = null;
  private FilterParser filters = null;
  private KitParser kits = null;
  private FeatureDefinitionContext features = null;

  public MapContextLegacy(MapContext map) {
    this.map = new WeakReference<>(map);
  }

  public MapContext getMap() {
    final MapContext map = this.map.get();
    if (map == null) {
      throw new MapNotFoundException(
          "Unknown map context was garbage collected (this should not happen?)");
    }
    return map;
  }

  public boolean isLegacy() {
    if (legacy == null) {
      legacy = getMap().getInfo().getProto().isOlderThan(ProtoVersions.FILTER_FEATURES);
    }
    return legacy;
  }

  @Override
  public RegionParser getRegions() {
    if (regions == null) {
      regions = isLegacy() ? new LegacyRegionParser(getMap()) : new FeatureRegionParser(getMap());
    }
    return regions;
  }

  @Override
  public FilterParser getFilters() {
    if (filters == null) {
      filters = isLegacy() ? new LegacyFilterParser(getMap()) : new FeatureFilterParser(getMap());
    }
    return filters;
  }

  @Override
  public KitParser getKits() {
    if (kits == null) {
      kits = isLegacy() ? new LegacyKitParser(getMap()) : new FeatureKitParser(getMap());
    }
    return kits;
  }

  @Override
  public FeatureDefinitionContext getFeatures() {
    if (features == null) {
      features = new FeatureDefinitionContext();
    }
    return features;
  }
}

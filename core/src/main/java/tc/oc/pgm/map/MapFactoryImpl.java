package tc.oc.pgm.map;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.JDOMParseException;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;
import tc.oc.pgm.api.module.ModuleGraph;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.parse.FeatureFilterParser;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.filters.parse.LegacyFilterParser;
import tc.oc.pgm.kits.FeatureKitParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.kits.LegacyKitParser;
import tc.oc.pgm.regions.FeatureRegionParser;
import tc.oc.pgm.regions.LegacyRegionParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapFactoryImpl extends ModuleGraph<MapModule<?>, MapModuleFactory<?>>
    implements MapFactory {

  private final Logger logger;
  private final MapSource source;
  private final MapIncludeProcessor includes;
  private Document document;
  private MapInfoImpl info;
  private RegionParser regions;
  private FilterParser filters;
  private KitParser kits;
  private FeatureDefinitionContext features;

  public MapFactoryImpl(Logger logger, MapSource source, MapIncludeProcessor includes) {
    super(Modules.MAP, Modules.MAP_DEPENDENCY_ONLY); // Don't copy, avoid N factory copies
    this.logger = ClassLogger.get(assertNotNull(logger), getClass(), assertNotNull(source).getId());
    this.source = source;
    this.includes = includes;
  }

  @Override
  protected MapModule<?> createModule(MapModuleFactory<?> factory) throws ModuleLoadException {
    try {
      return factory.parse(this, logger, document);
    } catch (InvalidXMLException e) {
      throw new ModuleLoadException(e);
    }
  }

  @Override
  public MapContext load() throws MapException {
    try {
      document = MapFilePreprocessor.getDocument(source, includes);

      info = new MapInfoImpl(source, document.getRootElement());
      try {
        loadAll();
      } catch (ModuleLoadException e) {
        if (e.getCause() instanceof InvalidXMLException) {
          throw e.getCause();
        }
        throw e;
      }
      postLoad();
    } catch (MapException e) {
      throw e;
    } catch (IOException e) {
      throw new MapException(source, info, "Unable to read map document", e);
    } catch (InvalidXMLException e) {
      throw new MapException(source, info, e.getMessage(), e);
    } catch (ModuleLoadException e) {
      throw new MapException(source, info, e.getFullMessage(), e);
    } catch (JDOMParseException e) {
      final InvalidXMLException cause = InvalidXMLException.fromJDOM(e, source.getId());
      throw new MapException(source, info, cause.getMessage(), cause);
    } catch (Throwable t) {
      throw new MapException(source, info, "Unhandled " + t.getClass().getName(), t);
    }

    return new MapContextImpl(info, getModules());
  }

  private void postLoad() throws InvalidXMLException {
    for (InvalidXMLException e : getFeatures().resolveReferences()) {
      throw e;
    }

    for (MapModule<?> module : getModules()) {
      module.postParse(this, logger, document);
    }
  }

  @Override
  public Version getProto() {
    if (info == null) {
      throw new IllegalStateException("Tried to access map proto before info was loaded");
    }
    return info.getProto();
  }

  private boolean isLegacy() {
    return getProto().isOlderThan(MapProtos.FILTER_FEATURES);
  }

  @Override
  public RegionParser getRegions() {
    if (regions == null) {
      regions = isLegacy() ? new LegacyRegionParser(this) : new FeatureRegionParser(this);
    }
    return regions;
  }

  @Override
  public FilterParser getFilters() {
    if (filters == null) {
      filters = isLegacy() ? new LegacyFilterParser(this) : new FeatureFilterParser(this);
    }
    return filters;
  }

  @Override
  public KitParser getKits() {
    if (kits == null) {
      kits = isLegacy() ? new LegacyKitParser(this) : new FeatureKitParser(this);
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

  @Override
  public Collection<String> getVariants() throws InvalidXMLException {
    Set<String> collect = new HashSet<>();
    for (Element variant : document.getRootElement().getChildren("variant")) {
      String id = XMLUtils.parseRequiredId(variant);
      if ("default".equals(id))
        throw new InvalidXMLException("Variant id must not be 'default'", variant);

      if (!collect.add(id))
        throw new InvalidXMLException("Duplicate variant ids are not allowed", variant);
    }
    return collect;
  }

  @Override
  public void close() {
    document = null;
    info = null;
    regions = null;
    filters = null;
    kits = null;
    features = null;
  }
}

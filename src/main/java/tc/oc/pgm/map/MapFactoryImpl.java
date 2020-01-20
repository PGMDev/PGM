package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.module.ModuleGraph;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
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
import tc.oc.util.Version;
import tc.oc.util.logging.ClassLogger;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.SAXHandler;

public class MapFactoryImpl extends ModuleGraph<MapModule, MapModuleFactory<? extends MapModule>>
    implements MapFactory {

  private static final SAXBuilder DOCUMENT_FACTORY = new SAXBuilder();

  static {
    DOCUMENT_FACTORY.setSAXHandlerFactory(SAXHandler.FACTORY);
  }

  private final Logger logger;
  private final MapSource source;
  private Document document;
  private MapInfo info;
  private RegionParser regions;
  private FilterParser filters;
  private KitParser kits;
  private FeatureDefinitionContext features;

  public MapFactoryImpl(Logger logger, MapSource source) {
    super(Modules.MAP); // Do not copy to avoid N copies of the factories
    this.logger = ClassLogger.get(checkNotNull(logger), getClass(), checkNotNull(source).getId());
    this.source = source;
  }

  @Override
  protected MapModule createModule(MapModuleFactory factory) throws ModuleLoadException {
    try {
      return factory.parse(this, logger, document);
    } catch (InvalidXMLException e) {
      throw new ModuleLoadException(e);
    }
  }

  private void preLoad() throws MapNotFoundException, ModuleLoadException {
    if (document != null && !source.checkForUpdates()) {
      return; // If a document is present and there are no updates, skip loading again
    }

    try (final InputStream stream = source.getDocument()) {
      document = DOCUMENT_FACTORY.build(stream);
      document.setBaseURI(source.getId());
      info = new MapInfoImpl(document.getRootElement());
    } catch (IOException e) {
      throw new MapNotFoundException(info, "Could not read document from " + source.getId(), e);
    } catch (InvalidXMLException e) {
      throw new ModuleLoadException(e);
    } catch (JDOMParseException e) {
      final InvalidXMLException cause = InvalidXMLException.fromJDOM(e, source.getId());
      throw new ModuleLoadException(cause.getMessage(), cause);
    } catch (Throwable t) {
      throw new ModuleLoadException(
          "Unhandled " + t.getClass().getName() + " from " + source.getId(), t);
    }
  }

  @Override
  public MapContext load() throws MapNotFoundException, ModuleLoadException {
    preLoad();
    try {
      loadAll();
    } catch (ModuleLoadException e) {
      unloadAll();
      throw e;
    }
    postLoad();
    return new MapContextImpl(info, source, getModules());
  }

  private void postLoad() throws ModuleLoadException {
    for (InvalidXMLException e : getFeatures().resolveReferences()) {
      throw new ModuleLoadException(e);
    }

    for (MapModule module : getModules()) {
      try {
        module.postParse(this, logger, document);
      } catch (InvalidXMLException e) {
        throw new ModuleLoadException(e);
      }
    }
  }

  @Override
  public Version getProto() {
    if (info == null) {
      throw new IllegalStateException("Tried to access map proto before info is loaded");
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
}

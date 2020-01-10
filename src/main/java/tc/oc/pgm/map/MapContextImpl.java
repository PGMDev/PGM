package tc.oc.pgm.map;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jdom2.Document;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.module.ModuleGraph;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.util.logging.ClassLogger;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.SAXHandler;

public class MapContextImpl extends ModuleGraph<MapModule, MapModuleFactory> implements MapContext {

  private static final SAXBuilder DOCUMENT_FACTORY = new SAXBuilder();

  static {
    DOCUMENT_FACTORY.setSAXHandlerFactory(SAXHandler.FACTORY);
  }

  private final Logger logger;
  private final AtomicReference<MapInfo> info;
  private final SoftReference<MapSource> source;
  private final AtomicBoolean loaded;
  private final AtomicReference<Document> document;
  private final AtomicReference<Legacy> legacy;
  private final Map<Class<? extends MapModule>, MapModule> modules;

  public MapContextImpl(Logger logger, MapSource source) {
    super(
        PGM.get()
            .getModuleRegistry()
            .getMapModuleFactories()); // Do not copy to avoid N copies of the factories
    this.logger = ClassLogger.get(logger, getClass(), source.getId());
    this.source = new SoftReference<>(source);
    this.loaded = new AtomicBoolean();
    this.document = new AtomicReference<>();
    this.info = new AtomicReference<>();
    this.legacy = new AtomicReference<>();
    this.modules = new ConcurrentHashMap<>();
  }

  @Override
  public MapInfo getInfo() {
    return info.get();
  }

  @Override
  public MapSource getSource() {
    return source.get();
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public boolean isLoaded() {
    return loaded.get();
  }

  private void preLoad() throws MapNotFoundException {
    final MapSource source = getSource();
    if (source == null) {
      throw new MapNotFoundException("Map source was garbage collected before pre-load");
    }

    if (isLoaded() && !source.checkForUpdates()) {
      return; // No need to reload document if already loaded and no updates
    }

    try (final InputStream stream = source.getDocument()) {
      document.set(DOCUMENT_FACTORY.build(stream));
      info.set(MapInfoImpl.parseInfo(document.get()));
    } catch (IOException e) {
      throw new MapNotFoundException(
          "Could not read document from map source: " + source.getId(), e);
    } catch (InvalidXMLException e) {
      throw new MapNotFoundException(e);
    } catch (JDOMParseException e) {
      final InvalidXMLException cause = InvalidXMLException.fromJDOM(e, source.getId());
      throw new MapNotFoundException("Error parsing document: " + cause.getMessage(), cause);
    } catch (Throwable t) {
      throw new MapNotFoundException(
          "Unhandled " + t.getClass().getName() + " parsing document: " + source.getId(), t);
    }
  }

  @Override
  protected Collection<MapModule> loadAll() throws ModuleLoadException {
    for (MapModule module : super.loadAll()) {
      modules.put(module.getClass(), module);
    }

    return getModules();
  }

  private void postLoad() throws MapNotFoundException {
    for (InvalidXMLException e : legacy().getFeatures().resolveReferences()) {
      throw new MapNotFoundException(e);
    }

    final Document doc = document.get();
    if (doc == null) {
      throw new MapNotFoundException(
          "Could not post-load document from map source: " + getSource());
    }

    for (MapModule module : getModules()) {
      try {
        module.postParse(this, logger, doc);
      } catch (InvalidXMLException e) {
        throw new MapNotFoundException(e);
      }
    }
  }

  @Override
  public void load() throws MapNotFoundException {
    loaded.set(true);
    try {
      preLoad(); // Skip pre-loading if already complete
      loadAll(); // Load all factories in dependency order
      postLoad(); // Resolve features and run post-load callbacks
    } catch (MapNotFoundException e) {
      unload();
      throw e;
    } catch (ModuleLoadException e) {
      unload();
      throw new MapNotFoundException(e);
    }
  }

  @Override
  public void unload() {
    if (loaded.compareAndSet(true, false)) {
      document.set(null);
      legacy.set(null);
      modules.clear();
    }
  }

  @Override
  public Legacy legacy() {
    if (legacy.get() == null) {
      legacy.set(new MapContextLegacy(this));
    }
    return legacy.get();
  }

  @Override
  public Collection<MapModule> getModules() {
    return ImmutableList.copyOf(modules.values());
  }

  @Override
  public <M extends MapModule> M getModule(Class<? extends M> key) {
    return (M) modules.get(key);
  }

  @Override
  protected MapModule getModule(MapModuleFactory factory) throws ModuleLoadException {
    final Document doc = document.get();
    if (doc == null) {
      throw new ModuleLoadException("Could not load document from map source: " + getSource());
    }

    try {
      return factory.parse(this, logger, doc);
    } catch (InvalidXMLException e) {
      throw new ModuleLoadException(e);
    }
  }

  @Override
  public int hashCode() {
    if (getInfo() == null) return 0;
    return getInfo().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapContext)) return false;
    if (getInfo() == null) return super.equals(obj);
    return Objects.equals(getInfo(), ((MapContext) obj).getInfo());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("info", getInfo())
        .append("source", getSource())
        .append("loaded", isLoaded())
        .build();
  }
}

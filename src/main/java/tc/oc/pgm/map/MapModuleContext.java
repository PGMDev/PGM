package tc.oc.pgm.map;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.plugin.Plugin;
import org.jdom2.Document;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.filters.FeatureFilterParser;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.LegacyFilterParser;
import tc.oc.pgm.kits.FeatureKitParser;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.kits.LegacyKitParser;
import tc.oc.pgm.maptag.MapTagSet;
import tc.oc.pgm.maptag.StandardMapTag;
import tc.oc.pgm.maptag.StandardMapTags;
import tc.oc.pgm.module.ModuleInfo;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.module.ModuleLoader;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.regions.FeatureRegionParser;
import tc.oc.pgm.regions.LegacyRegionParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.util.SemanticVersion;
import tc.oc.xml.InvalidXMLException;

/** Manages instanced modules in a specific context. */
public class MapModuleContext extends ModuleLoader<MapModule> {
  private final ModuleRegistry factory;
  private final Document doc;
  private final SemanticVersion proto;
  private final File basePath;

  // Sidebar title
  private @Nullable Component game = new PersonalizedTranslatable("match.scoreboard.default.title");

  private RegionParser regionParser;
  private FilterParser filterParser;
  private KitParser kitParser;

  private final FeatureDefinitionContext featureDefinitionContext;

  public MapModuleContext(
      Plugin plugin,
      ModuleRegistry factory,
      Document document,
      SemanticVersion proto,
      File basePath) {
    super(plugin.getLogger());
    this.factory = factory;
    this.doc = document;
    this.proto = proto;
    this.basePath = basePath;
    this.featureDefinitionContext = new FeatureDefinitionContext();
  }

  public MapPersistentContext generatePersistentContext() {
    MapInfo info = getInfo();
    int maxPlayers = 0;

    TeamModule tm = getModule(TeamModule.class);
    if (tm != null) {
      for (TeamFactory factory : getModule(TeamModule.class).getTeams()) {
        maxPlayers += factory.getMaxPlayers();
      }
    }

    FreeForAllModule ffam = getModule(FreeForAllModule.class);
    if (ffam != null) {
      maxPlayers += ffam.getOptions().maxPlayers;
    }

    MapTagSet mapTags = MapTagSet.mutable(info.mapTagSet);
    for (StandardMapTag standardMapTag : StandardMapTags.REGISTRY.getAll()) {
      if (standardMapTag.test(this)) {
        mapTags.add(standardMapTag);
      }
    }

    return new MapPersistentContext(proto, info, maxPlayers, mapTags);
  }

  public MapInfo getInfo() {
    return needModule(InfoModule.class).getMapInfo();
  }

  public File getBasePath() {
    return basePath;
  }

  public Component getGame() {
    Component game = needModule(InfoModule.class).getMapInfo().game;
    if (game != null) return game;
    return this.game;
  }

  @Override
  protected MapModule loadModule(ModuleInfo info) throws ModuleLoadException {
    MapModuleFactory<?> factory = this.factory.getModuleFactory(info);
    if (factory == null) throw new IllegalStateException("No factory registred for module " + info);
    MapModule newModule = null;
    try {
      newModule = factory.parse(this, logger, doc);
    } catch (InvalidXMLException e) {
      throw new ModuleLoadException(e);
    }
    if (newModule != null) {
      Component game = newModule.getGame(this);
      if (game != null) this.game = game;
    }
    return newModule;
  }

  public void load() {
    loadAll(factory.getModules(), false);

    // If all features resolved OK, call each module's post-resolve method
    Collection<ModuleLoadException> errors =
        featureDefinitionContext.resolveReferences().stream()
            .map(ModuleLoadException::new)
            .collect(Collectors.toList());
    addErrors(errors);

    if (errors.isEmpty()) {
      for (MapModule module : getModules()) {
        try {
          module.postParse(this, factory.getLogger(), doc);
        } catch (InvalidXMLException e) {
          addError(new ModuleLoadException(e));
        } catch (Throwable e) {
          addError(
              new ModuleLoadException(
                  module.getInfo(), "Post-parse exception from module " + module.getName(), e));
        }
      }
    }
  }

  public SemanticVersion getProto() {
    return proto;
  }

  public RegionParser getRegionParser() {
    if (this.regionParser == null) {
      if (getProto().isOlderThan(ProtoVersions.FILTER_FEATURES)) {
        this.regionParser = new LegacyRegionParser(this);
      } else {
        this.regionParser = new FeatureRegionParser(this);
      }
    }
    return this.regionParser;
  }

  public FilterParser getFilterParser() {
    if (this.filterParser == null) {
      if (getProto().isOlderThan(ProtoVersions.FILTER_FEATURES)) {
        this.filterParser = new LegacyFilterParser(this);
      } else {
        this.filterParser = new FeatureFilterParser(this);
      }
    }
    return this.filterParser;
  }

  public KitParser getKitParser() {
    if (this.kitParser == null) {
      if (getProto().isOlderThan(ProtoVersions.FILTER_FEATURES)) {
        this.kitParser = new LegacyKitParser(this);
      } else {
        this.kitParser = new FeatureKitParser(this);
      }
    }
    return this.kitParser;
  }

  public FeatureDefinitionContext features() {
    return this.featureDefinitionContext;
  }

  public <T extends MapModule> T needModule(Class<T> moduleClass) {
    T module = moduleClass.cast(getModule(moduleClass));
    if (module == null) {
      throw new IllegalStateException(
          "Module "
              + moduleClass.getSimpleName()
              + " is required at this point but has not yet loaded");
    }
    return module;
  }
}

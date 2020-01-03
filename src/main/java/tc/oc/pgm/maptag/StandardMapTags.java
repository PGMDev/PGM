package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import tc.oc.pgm.api.registry.IRegistry;
import tc.oc.pgm.api.registry.Registry;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.controlpoint.ControlPointModule;
import tc.oc.pgm.core.CoreModule;
import tc.oc.pgm.crafting.CraftingModule;
import tc.oc.pgm.destroyable.DestroyableModule;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.flag.FlagModule;
import tc.oc.pgm.hunger.HungerModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.modules.InternalModule;
import tc.oc.pgm.modules.LaneModule;
import tc.oc.pgm.rage.RageModule;
import tc.oc.pgm.score.ScoreModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.terrain.TerrainModule;
import tc.oc.pgm.timelimit.TimeLimitModule;
import tc.oc.pgm.tnt.TNTModule;
import tc.oc.pgm.wool.WoolModule;
import tc.oc.pgm.worldborder.WorldBorderModule;
import tc.oc.util.logging.ClassLogger;

public final class StandardMapTags {

  private StandardMapTags() {}

  public static final IRegistry<StandardMapTag> REGISTRY = new Registry<>(new LinkedHashMap<>());

  public static final StandardMapTag _4TEAMS =
      create("4teams", TeamModule.class, team -> team.getTeams().size() == 4);
  public static final StandardMapTag AUTOTNT =
      create("autotnt", TNTModule.class, tnt -> tnt.getProperties().instantIgnite);
  public static final StandardMapTag BLITZ =
      create("blitz", BlitzModule.class, blitz -> !blitz.isDisabled(null));
  public static final StandardMapTag CLASSES = create("classes", ClassModule.class);
  public static final StandardMapTag CONTROLPOINT =
      create("controlpoint", ControlPointModule.class);
  public static final StandardMapTag CORE = create("core", CoreModule.class);
  public static final StandardMapTag CRAFTING =
      create("crafting", CraftingModule.class, crafting -> !crafting.getCustomRecipes().isEmpty());
  public static final StandardMapTag DEATHMATCH =
      create(
          "deathmatch",
          ScoreModule.class,
          score -> score.getConfig().deathScore != 0 || score.getConfig().killScore != 0);
  public static final StandardMapTag EVENTEAMS =
      create("eventeams", TeamModule.class, team -> team.shouldRequireEven().orElse(false));
  public static final StandardMapTag FFA = create("ffa", FreeForAllModule.class);
  public static final StandardMapTag FLAG = create("flag", FlagModule.class);
  public static final StandardMapTag FRIENDLYFIRE =
      create("friendlyfire", InfoModule.class, info -> info.getMapInfo().friendlyFire);
  public static final StandardMapTag INTERNAL = create("internal", InternalModule.class);
  public static final StandardMapTag MONUMENT = create("monument", DestroyableModule.class);
  public static final StandardMapTag NOHUNGER = create("nohunger", HungerModule.class);
  public static final StandardMapTag RACEFORWOOL = create("raceforwool", LaneModule.class);
  public static final StandardMapTag RAGE = create("rage", RageModule.class);
  public static final StandardMapTag SCOREBOX =
      create("scorebox", ScoreModule.class, score -> !score.getScoreBoxFactories().isEmpty());
  public static final StandardMapTag TEAMS = create("teams", TeamModule.class);
  public static final StandardMapTag TIMELIMIT =
      create("timelimit", TimeLimitModule.class, timeLimit -> timeLimit.getTimeLimit().isPresent());
  public static final StandardMapTag VANILLAWORLDGEN =
      create("vanillaworldgen", TerrainModule.class, terrain -> terrain.getOptions().vanilla);
  public static final StandardMapTag WOOL = create("wool", WoolModule.class);
  public static final StandardMapTag WORLDBORDER = create("worldborder", WorldBorderModule.class);

  static {
    try {
      for (StandardMapTag standardMapTag : collect()) {
        REGISTRY.register(standardMapTag.getName(), standardMapTag);
      }
    } catch (IllegalAccessException e) {
      Logger logger = ClassLogger.get(StandardMapTags.class);
      logger.log(Level.SEVERE, "Could not register standard map tags.", e);
    }
  }

  private static StandardMapTag create(String name, Predicate<MapModuleContext> ifApplicable) {
    checkNotNull(name);
    checkNotNull(ifApplicable);
    return new StandardMapTag(name, ifApplicable);
  }

  private static <T extends MapModule> StandardMapTag create(String name, Class<T> moduleClass) {
    return create(name, moduleClass, null);
  }

  private static <T extends MapModule> StandardMapTag create(
      String name, Class<T> moduleClass, Predicate<T> ifApplicable) {
    checkNotNull(name);
    checkNotNull(moduleClass);

    Predicate<T> finalIfApplicable = ifApplicable != null ? ifApplicable : module -> true;
    return create(
        name,
        mapModuleContext -> {
          T module = mapModuleContext.getModule(moduleClass);
          return module != null && finalIfApplicable.test(module);
        });
  }

  private static Set<StandardMapTag> collect() throws IllegalAccessException {
    SortedSet<StandardMapTag> sorted = new TreeSet<>(Comparator.naturalOrder());
    for (Field field : StandardMapTags.class.getFields()) {
      int modifiers = field.getModifiers();
      if (Modifier.isPublic(modifiers)
          && Modifier.isStatic(modifiers)
          && Modifier.isFinal(modifiers)) {
        if (StandardMapTag.class.isAssignableFrom(field.getType())) {
          sorted.add((StandardMapTag) field.get(null));
        }
      }
    }

    return ImmutableSet.copyOf(sorted);
  }
}

package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
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
import tc.oc.pgm.map.PGMMap;
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

public interface StandardMapTags {
  StandardMapTag _4TEAMS = create("4teams", TeamModule.class, team -> team.getTeams().size() == 4);
  StandardMapTag AUTOTNT =
      create("autotnt", TNTModule.class, tnt -> tnt.getProperties().instantIgnite);
  StandardMapTag BLITZ = create("blitz", BlitzModule.class, blitz -> !blitz.isDisabled(null));
  StandardMapTag CLASSES = create("classes", ClassModule.class);
  StandardMapTag CORE = create("core", CoreModule.class);
  StandardMapTag CRAFTING =
      create("crafting", CraftingModule.class, crafting -> !crafting.getCustomRecipes().isEmpty());
  StandardMapTag CTP = create("ctp", ControlPointModule.class);
  StandardMapTag EVENTEAMS =
      create("eventeams", TeamModule.class, team -> team.shouldRequireEven().orElse(false));
  StandardMapTag FFA = create("ffa", FreeForAllModule.class);
  StandardMapTag FLAG = create("flag", FlagModule.class);
  StandardMapTag FRIENDLYFIRE =
      create("friendlyfire", InfoModule.class, info -> info.getMapInfo().friendlyFire);
  StandardMapTag INTERNAL = create("internal", InternalModule.class);
  StandardMapTag MONUMENT = create("monument", DestroyableModule.class);
  StandardMapTag NOHUNGER = create("nohunger", HungerModule.class);
  StandardMapTag RAGE = create("rage", RageModule.class);
  StandardMapTag RFW = create("rfw", LaneModule.class);
  StandardMapTag SCOREBOX =
      create("scorebox", ScoreModule.class, score -> !score.getScoreBoxFactories().isEmpty());
  StandardMapTag TDM =
      create(
          "tdm",
          ScoreModule.class,
          score -> score.getConfig().deathScore != 0 || score.getConfig().killScore != 0);
  StandardMapTag TEAMS = create("teams", TeamModule.class);
  StandardMapTag TIMELIMIT =
      create("timelimit", TimeLimitModule.class, timeLimit -> timeLimit.getTimeLimit().isPresent());
  StandardMapTag VANILLAWORLDGEN =
      create("vanillaworldgen", TerrainModule.class, terrain -> terrain.getOptions().vanilla);
  StandardMapTag WOOL = create("wool", WoolModule.class);
  StandardMapTag WORLDBORDER = create("worldborder", WorldBorderModule.class);

  static StandardMapTag create(String name, Predicate<PGMMap> ifApplicable) {
    checkNotNull(name);
    checkNotNull(ifApplicable);
    return new StandardMapTag(name, ifApplicable);
  }

  static <T extends MapModule> StandardMapTag create(String name, Class<T> moduleClass) {
    return create(name, moduleClass, null);
  }

  static <T extends MapModule> StandardMapTag create(
      String name, Class<T> moduleClass, Predicate<T> ifApplicable) {
    checkNotNull(name);
    checkNotNull(moduleClass);

    Predicate<T> finalIfApplicable = ifApplicable != null ? ifApplicable : module -> true;
    return create(
        name,
        map ->
            map.getContext()
                .map(context -> context.getModule(moduleClass))
                .map(finalIfApplicable::test)
                .orElse(false));
  }

  static Set<StandardMapTag> collect(Class<?> clazz) throws IllegalAccessException {
    SortedSet<StandardMapTag> sorted = new TreeSet<>(Comparator.naturalOrder());
    for (Field field : clazz.getFields()) {
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

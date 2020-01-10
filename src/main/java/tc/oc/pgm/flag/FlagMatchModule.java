package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.goals.GoalMatchModule;

public class FlagMatchModule implements MatchModule, Listener {

  private final ImmutableMap<FlagDefinition, Flag> flags;

  public FlagMatchModule(
      Match match, ImmutableList<Net> nets, ImmutableList<FlagDefinition> flagDefinitions)
      throws ModuleLoadException {

    ImmutableMap.Builder<FlagDefinition, Flag> flags = ImmutableMap.builder();
    for (FlagDefinition definition : flagDefinitions) {
      ImmutableSet.Builder<Net> netsBuilder = ImmutableSet.builder();
      for (Net net : nets) {
        if (net.getCapturableFlags().contains(definition)) {
          netsBuilder.add(net);
        }
      }

      Flag flag = new Flag(match, definition, netsBuilder.build());
      flags.put(definition, flag);
      match.getFeatureContext().add(flag);
      match.needMatchModule(GoalMatchModule.class).addGoal(flag);
    }
    this.flags = flags.build();
  }

  @Override
  public void load() {
    for (Flag flag : this.flags.values()) {
      flag.load();
    }
  }

  public ImmutableCollection<Flag> getFlags() {
    return flags.values();
  }
}

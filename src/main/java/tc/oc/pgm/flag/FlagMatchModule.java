package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleLoadException;

public class FlagMatchModule extends MatchModule implements Listener {

  private final ImmutableMap<FlagDefinition, Flag> flags;

  public FlagMatchModule(
      Match match, ImmutableList<Net> nets, ImmutableList<FlagDefinition> flagDefinitions)
      throws ModuleLoadException {
    super(match);

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
    super.load();
    for (Flag flag : this.flags.values()) {
      flag.load();
    }
  }

  public ImmutableCollection<Flag> getFlags() {
    return flags.values();
  }
}

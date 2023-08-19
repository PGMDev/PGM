package tc.oc.pgm.variables;

import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

public class VariablesMatchModule implements MatchModule, Listener {

  private final Match match;

  public VariablesMatchModule(Match match) {
    this.match = match;
  }

  @Override
  public void load() throws ModuleLoadException {
    match.getFeatureContext().getAll(Variable.class).forEach(v -> v.postLoad(match));
  }
}

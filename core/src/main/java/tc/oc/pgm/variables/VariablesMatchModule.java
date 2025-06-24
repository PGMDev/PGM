package tc.oc.pgm.variables;

import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.util.collection.ContextStore;

@ListenerScope(MatchScope.LOADED)
public class VariablesMatchModule implements MatchModule, Listener {

  private final Match match;
  private final FeatureDefinitionContext context;

  public VariablesMatchModule(Match match, FeatureDefinitionContext context) {
    this.match = match;
    this.context = context;
  }

  public Stream<Map.Entry<String, Variable<?>>> getVariables() {
    //noinspection unchecked
    return ((ContextStore<? super Variable<?>>) context)
        .stream().filter(e -> e.getValue() instanceof Variable<?>).map(e ->
            (Map.Entry<String, Variable<?>>) e);
  }

  public String getId(Variable<?> variable) {
    return context.getName(variable);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMatchLoad(MatchLoadEvent event) {
    for (Variable var : context.getAll(Variable.class)) {
      var.load(match);
    }
  }
}

package tc.oc.pgm.action;

import com.google.common.collect.ImmutableList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;

public class ActionMatchModule implements MatchModule {
  private final Match match;
  private final ImmutableList<Trigger<?>> triggers;

  public ActionMatchModule(Match match, ImmutableList<Trigger<?>> triggers) {
    this.match = match;
    this.triggers = triggers;
  }

  @Override
  public void load() throws ModuleLoadException {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    for (Trigger<?> trigger : triggers) {
      setupTrigger(trigger, fmm);
    }
  }

  private <T extends Filterable<?>> void setupTrigger(Trigger<T> rule, FilterMatchModule fmm) {
    fmm.onChange(
        rule.getScope(),
        rule.getFilter(),
        (filterable, response) -> {
          if (response) rule.getAction().trigger(filterable);
          else rule.getAction().untrigger(filterable);
        });
  }
}

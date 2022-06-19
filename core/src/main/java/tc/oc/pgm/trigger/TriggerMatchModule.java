package tc.oc.pgm.trigger;

import com.google.common.collect.ImmutableList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;
import tc.oc.pgm.filters.dynamic.Filterable;

public class TriggerMatchModule implements MatchModule {
  private final Match match;
  private final ImmutableList<TriggerRule<?>> triggerRules;

  public TriggerMatchModule(Match match, ImmutableList<TriggerRule<?>> triggerRules) {
    this.match = match;
    this.triggerRules = triggerRules;
  }

  @Override
  public void load() throws ModuleLoadException {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    for (TriggerRule<?> triggerRule : triggerRules) {
      setupTrigger(triggerRule, fmm);
    }
  }

  private <T extends Filterable<?>> void setupTrigger(TriggerRule<T> rule, FilterMatchModule fmm) {
    fmm.onChange(
        rule.getScope(),
        rule.getFilter(),
        (t, val) -> {
          if (val) rule.getTrigger().trigger(t);
          else rule.getTrigger().untrigger(t);
        });
  }
}

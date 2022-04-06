package tc.oc.pgm.kits;

import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.kits.Kit;

public class KitRule implements FeatureDefinition {

  enum Action {
    GIVE,
    TAKE,
    LEND
  }

  private final Action action;
  private final Kit kit;
  private final Filter filter;

  public KitRule(Action action, Kit kit, Filter filter) {
    this.action = action;
    this.kit = kit;
    this.filter = filter;
  }

  public Action getAction() {
    return action;
  }

  public Kit getKit() {
    return kit;
  }

  public Filter getFilter() {
    return filter;
  }
}

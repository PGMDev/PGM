package tc.oc.pgm.filters.matcher.match;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.flag.state.State;

public class FlagStateFilter extends TypedFilter.Impl<MatchQuery> {

  private final FeatureReference<? extends FlagDefinition> flag;
  private final @Nullable FeatureReference<? extends PostDefinition> post;
  private final Class<? extends State> state;

  public FlagStateFilter(
      FeatureReference<? extends FlagDefinition> flag,
      @Nullable FeatureReference<? extends PostDefinition> post,
      Class<? extends State> state) {
    this.flag = flag;
    this.post = post;
    this.state = state;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(FlagStateChangeEvent.class);
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    Flag flag = this.flag.get().getGoal(query.getMatch());
    // FIXME: This may occur at load time. We need a better fix for this.
    if (flag == null) return false;

    return flag.isCurrent(this.state) && (this.post == null || flag.isAtPost(this.post.get()));
  }
}

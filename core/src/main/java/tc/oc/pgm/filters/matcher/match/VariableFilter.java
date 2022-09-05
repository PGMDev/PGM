package tc.oc.pgm.filters.matcher.match;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.WeakTypedFilter;
import tc.oc.pgm.variables.VariableDefinition;

public class VariableFilter<T extends Filterable<?>> implements WeakTypedFilter<MatchQuery> {

  private final VariableDefinition<T> variable;
  private final Range<Double> values;

  public VariableFilter(VariableDefinition<T> variable, Range<Double> values) {
    this.variable = variable;
    this.values = values;
  }

  @Override
  public boolean isDynamic() {
    // Variables' setValue will always invalidate the filterable directly, no events required
    return true;
  }

  @Override
  public Class<MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public QueryResponse queryTyped(MatchQuery query) {
    Filterable<?> filterable = query.extractFilterable();
    if (!respondsTo(filterable.getClass())) return QueryResponse.ABSTAIN;

    return QueryResponse.fromBoolean(
        values.contains(variable.getVariable(query.getMatch()).getValue(filterable)));
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    //noinspection unchecked
    return Filterable.class.isAssignableFrom(queryType)
        && Filterables.isAssignable((Class<Filterable<?>>) queryType, variable.getScope());
  }

  @Override
  public String toString() {
    return "VariableFilter{" + "variable=" + variable + ", values=" + values + '}';
  }
}

package tc.oc.pgm.filters.matcher.match;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.VariableDefinition;

public class VariableFilter implements FilterDefinition {

  private final VariableDefinition<?> variable;
  private final Range<Double> values;

  public VariableFilter(VariableDefinition<?> variable, Range<Double> values) {
    this.variable = variable;
    this.values = values;
  }

  @Override
  public QueryResponse query(Query query) {
    Filterable<?> filterable =
        query instanceof MatchQuery ? ((MatchQuery) query).extractFilterable() : null;
    if (!Filterables.isAssignable(filterable, variable.getScope())) return QueryResponse.ABSTAIN;

    return QueryResponse.fromBoolean(
        values.contains(variable.getVariable(filterable.getMatch()).getValue(filterable)));
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    //noinspection unchecked
    return Filterable.class.isAssignableFrom(queryType)
        && Filterables.isAssignable((Class<Filterable<?>>) queryType, variable.getScope());
  }

  @Override
  public boolean isDynamic() {
    // Variables' setValue will always invalidate the filterable directly, no events required
    return true;
  }

  @Override
  public String toString() {
    return "VariableFilter{" + "variable=" + variable + ", values=" + values + '}';
  }
}

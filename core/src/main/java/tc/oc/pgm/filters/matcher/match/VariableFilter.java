package tc.oc.pgm.filters.matcher.match;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.WeakTypedFilter;
import tc.oc.pgm.filters.matcher.party.CompetitorFilter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.variables.Variable;

public abstract class VariableFilter<Q extends MatchQuery> implements WeakTypedFilter<Q> {

  private final Variable<?> variable;
  private final Range<Double> values;

  private VariableFilter(Variable<?> variable, Range<Double> values) {
    this.variable = variable;
    this.values = values;
  }

  public static VariableFilter<?> of(Variable<?> var, Integer idx, Range<Double> range, Node node)
      throws InvalidXMLException {
    if (var.isIndexed()) {
      if (idx == null)
        throw new InvalidXMLException("Array variables must contain an index.", node);
      return var.getScope() == Party.class
          ? new TeamIndexed(var, idx, range)
          : new Indexed(var, idx, range);
    } else {
      if (idx != null)
        throw new InvalidXMLException("Non-array variables cannot contain an index.", node);
      return var.getScope() == Party.class ? new Team(var, range) : new Generic(var, range);
    }
  }

  @Override
  public QueryResponse queryTyped(Q query) {
    Filterable<?> filterable = query.extractFilterable();
    if (!Filterables.isAssignable(filterable, variable.getScope())) return QueryResponse.ABSTAIN;

    return QueryResponse.fromBoolean(values.contains(getValue(variable, filterable)));
  }

  protected double getValue(Variable<?> variable, Filterable<?> filterable) {
    return variable.getValue(filterable);
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    //noinspection unchecked
    return Filterable.class.isAssignableFrom(queryType)
        && Filterables.isAssignable((Class<Filterable<?>>) queryType, variable.getScope());
  }

  @Override
  public boolean isDynamic() {
    return variable.isDynamic();
  }

  @Override
  public String toString() {
    return "VariableFilter{" + "variable=" + variable + ", values=" + values + '}';
  }

  public static class Generic extends VariableFilter<MatchQuery> {

    public Generic(Variable<?> variable, Range<Double> values) {
      super(variable, values);
    }

    @Override
    public Class<? extends MatchQuery> queryType() {
      return MatchQuery.class;
    }
  }

  /**
   * Specialization for team variables implementing CompetitorFilter. Allows team to be set to a
   * specific one.
   */
  public static class Team extends VariableFilter<PartyQuery> implements CompetitorFilter {

    public Team(Variable<?> variable, Range<Double> values) {
      super(variable, values);
    }

    @Override
    public boolean matches(MatchQuery query, Competitor competitor) {
      QueryResponse response = super.query(competitor);
      if (!response.isPresent())
        throw new UnsupportedOperationException(
            "Filter " + this + " did not respond to the query " + query);
      return response.isAllowed();
    }
  }

  public static class Indexed extends Generic {
    private final int idx;

    public Indexed(Variable<?> variable, int idx, Range<Double> values) {
      super(variable, values);
      this.idx = idx;
    }

    @Override
    protected double getValue(Variable<?> variable, Filterable<?> filterable) {
      return ((Variable.Indexed<?>) variable).getValue(filterable, idx);
    }
  }

  public static class TeamIndexed extends Team {
    private final int idx;

    public TeamIndexed(Variable<?> variable, int idx, Range<Double> values) {
      super(variable, values);
      this.idx = idx;
    }

    @Override
    protected double getValue(Variable<?> variable, Filterable<?> filterable) {
      return ((Variable.Indexed<?>) variable).getValue(filterable, idx);
    }
  }
}

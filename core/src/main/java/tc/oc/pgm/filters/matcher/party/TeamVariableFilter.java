package tc.oc.pgm.filters.matcher.party;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.filters.matcher.match.VariableFilter;
import tc.oc.pgm.variables.VariableDefinition;

public class TeamVariableFilter extends VariableFilter implements CompetitorFilter {

  public TeamVariableFilter(VariableDefinition<?> variable, Range<Double> values) {
    super(variable, values);
  }

  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
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

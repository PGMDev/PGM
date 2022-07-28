package tc.oc.pgm.api.filter;

import tc.oc.pgm.api.feature.FeatureDefinitionException;
import tc.oc.pgm.api.filter.query.Query;

public class FilterTypeException extends FeatureDefinitionException {

  private final Class<? extends Query> queryType;

  public FilterTypeException(Filter filter, Class<? extends Query> queryType) {
    super(
        "Filter type "
            + filter.getDefinitionType().getSimpleName()
            + " cannot respond to queries of type "
            + queryType.getSimpleName(),
        filter);
    this.queryType = queryType;
  }

  public Class<? extends Query> queryType() {
    return queryType;
  }
}

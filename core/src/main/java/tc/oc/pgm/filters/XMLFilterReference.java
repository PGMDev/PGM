package tc.oc.pgm.filters;

import javax.annotation.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.Node;

/** A {@link Filter} that delegates all methods to an XML reference */
public class XMLFilterReference extends XMLFeatureReference<FilterDefinition> implements Filter {

  public XMLFilterReference(
      FeatureDefinitionContext context, Node node, Class<FilterDefinition> type) {
    this(context, node, null, type);
  }

  public XMLFilterReference(
      FeatureDefinitionContext context,
      Node node,
      @Nullable String id,
      Class<FilterDefinition> type) {
    super(context, node, id, type);
  }

  @Override
  public Class<? extends Query> getQueryType() {
    return get().getQueryType();
  }

  @Override
  public QueryResponse query(Query query) {
    return get().query(query);
  }
}

package tc.oc.pgm.filters;

import javax.annotation.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.query.IQuery;
import tc.oc.xml.Node;

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
  public Class<? extends IQuery> getQueryType() {
    return get().getQueryType();
  }

  @Override
  public QueryResponse query(IQuery query) {
    return get().query(query);
  }
}

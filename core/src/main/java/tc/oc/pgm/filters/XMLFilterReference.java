package tc.oc.pgm.filters;

import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.xml.FeatureDefinitionContext;
import tc.oc.pgm.api.xml.Node;
import tc.oc.pgm.api.xml.XMLFeatureReference;

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
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return this.get().getRelevantEvents();
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

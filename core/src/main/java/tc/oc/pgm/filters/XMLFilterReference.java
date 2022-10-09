package tc.oc.pgm.filters;

import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.Node;

/** A {@link Filter} that delegates all methods to an XML reference */
public class XMLFilterReference extends XMLFeatureReference<FilterDefinition> implements Filter {

  public XMLFilterReference(FeatureDefinitionContext context, Node node, @Nullable String id) {
    super(context, node, id, FilterDefinition.class);
  }

  @Override
  public QueryResponse query(Query query) {
    return get().query(query);
  }

  @Override
  public boolean response(Query query) {
    return get().response(query);
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    return get().respondsTo(queryType);
  }

  @Override
  public boolean isDynamic() {
    return get().isDynamic();
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return get().getRelevantEvents();
  }

  @Nullable
  @Override
  public Class<? extends FeatureDefinition> getDefinitionType() {
    return get().getDefinitionType();
  }

  @Override
  public Stream<? extends FeatureDefinition> dependencies() {
    return get().dependencies();
  }
}

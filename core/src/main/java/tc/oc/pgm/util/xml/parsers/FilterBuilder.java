package tc.oc.pgm.util.xml.parsers;

import org.jdom2.Element;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.variables.LocalScope;

public class FilterBuilder extends Builder<Filter, FilterBuilder> {
  private final FilterParser filters;
  private final @Nullable LocalScope locals;

  public FilterBuilder(FilterParser filters, Element el, String... prop) {
    this(filters, null, el, prop);
  }

  public FilterBuilder(
      FilterParser filters, @Nullable LocalScope locals, Element el, String... prop) {
    super(el, prop);
    this.filters = filters;
    this.locals = locals;
  }

  public FilterBuilder dynamic() {
    validate((f, n) -> filters.validate(f, DynamicFilterValidation.ANY, n));
    return this;
  }

  public FilterBuilder dynamic(Class<? extends Filterable<?>> type) {
    validate((f, n) -> filters.validate(f, DynamicFilterValidation.of(type), n));
    return this;
  }

  public Filter orAllow() throws InvalidXMLException {
    return optional(StaticFilter.ALLOW);
  }

  public Filter orDeny() throws InvalidXMLException {
    return optional(StaticFilter.DENY);
  }

  public Filter result(boolean result) throws InvalidXMLException {
    return optional(result ? StaticFilter.ALLOW : StaticFilter.DENY);
  }

  @Override
  protected Filter parse(Node node) throws InvalidXMLException {
    if (prop.length == 0) return filters.parse(el, locals);
    return filters.parseProperty(node, locals);
  }

  @Override
  protected FilterBuilder getThis() {
    return this;
  }
}

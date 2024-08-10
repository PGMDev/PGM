package tc.oc.pgm.command.parsers;

import java.util.Map;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.ParserParameters;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.FilterMatchModule;

public final class FilterParser
    extends MatchObjectParser<Filter, Map.Entry<String, Filter>, FilterMatchModule> {

  public FilterParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Filter.class, FilterMatchModule.class, "filters");
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Iterable<Map.Entry<String, Filter>> objects(FilterMatchModule module) {
    return () -> module.getFilterContext().stream()
        .filter(entry -> entry != null && entry.getValue() instanceof Filter)
        .map(entry -> (Map.Entry<String, Filter>) entry)
        .iterator();
  }

  @Override
  protected String getName(Map.Entry<String, Filter> obj) {
    return obj.getKey();
  }

  @Override
  protected Filter getValue(Map.Entry<String, Filter> obj) {
    return obj.getValue();
  }
}

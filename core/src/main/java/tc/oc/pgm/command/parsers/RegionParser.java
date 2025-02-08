package tc.oc.pgm.command.parsers;

import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.ParserParameters;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterMatchModule;

public final class RegionParser
    extends MatchObjectParser<Region, Map.Entry<String, Region>, FilterMatchModule> {

  public RegionParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Region.class, FilterMatchModule.class, "regions");
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Iterable<Map.Entry<String, Region>> objects(FilterMatchModule module) {
    if (!(module.getFilterContext() instanceof FeatureDefinitionContext fdc)) return List.of();

    return () -> fdc.stream()
        .filter(entry -> entry != null && entry.getValue() instanceof Region)
        .map(entry -> (Map.Entry<String, Region>) (Map.Entry<?, ?>) entry)
        .iterator();
  }

  @Override
  protected String getName(Map.Entry<String, Region> obj) {
    return obj.getKey();
  }

  @Override
  protected Region getValue(Map.Entry<String, Region> obj) {
    return obj.getValue();
  }
}

package tc.oc.pgm.command.parsers;

import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.ParserParameters;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterMatchModule;

public final class RegionParser
    extends MatchObjectParser<Region, Map.Entry<String, FeatureDefinition>, FilterMatchModule> {

  public RegionParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Region.class, FilterMatchModule.class, "regions");
  }

  @Override
  protected Iterable<Map.Entry<String, FeatureDefinition>> objects(FilterMatchModule module) {
    if (!(module.getFilterContext() instanceof FeatureDefinitionContext fdc)) return List.of();
    return () -> fdc.stream()
        .filter(entry -> entry != null && entry.getValue() instanceof Region)
        .iterator();
  }

  @Override
  protected String getName(Map.Entry<String, FeatureDefinition> obj) {
    return obj.getKey();
  }

  @Override
  protected Region getValue(Map.Entry<String, FeatureDefinition> obj) {
    return (Region) obj.getValue();
  }
}

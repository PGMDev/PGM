package tc.oc.pgm.loot;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.itemmeta.ItemModifyMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.compose.Composition;
import tc.oc.pgm.util.compose.CompositionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class LootableModule implements MapModule<LootableMatchModule> {

  private final List<FillerDefinition> fillers;
  private final List<Cache> caches;

  public LootableModule(List<FillerDefinition> fillers, List<Cache> caches) {
    this.fillers = fillers;
    this.caches = caches;
  }

  @Nullable
  @Override
  public LootableMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new LootableMatchModule(match.getLogger(), match, this.fillers, this.caches);
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getWeakDependencies() {
    return ImmutableList.of(ItemModifyMatchModule.class);
  }

  public static class Factory implements MapModuleFactory<LootableModule> {

    @Nullable
    @Override
    public LootableModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<FillerDefinition> fillers = new ArrayList<>();
      List<Cache> caches = new ArrayList<>();
      CompositionParser<ItemStack> lootParser = new LootParser(factory);

      for (Element loot : XMLUtils.flattenElements(doc.getRootElement(), "lootables", "loot")) {
        factory
            .getFeatures()
            .addFeature(
                loot,
                new Loot(
                    XMLUtils.getRequiredAttribute(loot, "id").getValue(),
                    lootParser.parseElement(loot)));
      }

      FilterParser filters = factory.getFilters();

      for (Element filler : XMLUtils.flattenElements(doc.getRootElement(), "lootables", "fill")) {
        Composition<ItemStack> loot =
            factory
                .getFeatures()
                .get(XMLUtils.getRequiredAttribute(filler, "loot").getValue(), Loot.class)
                .lootItems();
        Filter fillableFilter = filters.parseFilterProperty(filler, "filter", StaticFilter.ALLOW);
        Duration refillInterval =
            XMLUtils.parseDuration(
                filler.getAttribute("refill-interval"), TimeUtils.INFINITE_DURATION);
        Filter refillTrigger =
            filters.parseProperty(
                filler, "refill-trigger", StaticFilter.DENY, DynamicFilterValidation.MATCH);
        if (refillTrigger == StaticFilter.DENY && refillInterval == TimeUtils.INFINITE_DURATION)
          throw new InvalidXMLException(
              "Lootable filler needs either a refill trigger or a refill interval", filler);

        boolean cleanBeforeRefill =
            XMLUtils.parseBoolean(filler.getAttribute("refill-clear"), true);

        FillerDefinition fillerDefinition =
            new FillerDefinition(
                loot, fillableFilter, refillTrigger, refillInterval, cleanBeforeRefill);
        factory.getFeatures().addFeature(filler, fillerDefinition);
        fillers.add(fillerDefinition);
      }

      for (Element cache : XMLUtils.flattenElements(doc.getRootElement(), "lootables", "cache")) {
        Region region = factory.getRegions().parseRequiredRegionProperty(cache, "region");
        Filter filter = filters.parseFilterProperty(cache, "filter", StaticFilter.ALLOW);

        Cache parsedCache = new Cache(region, filter);
        factory.getFeatures().addFeature(cache, parsedCache);
        caches.add(parsedCache);
      }

      return fillers.isEmpty() ? null : new LootableModule(fillers, caches);
    }
  }
}

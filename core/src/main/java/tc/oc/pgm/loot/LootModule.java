package tc.oc.pgm.loot;

import com.google.common.collect.Range;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class LootModule implements MapModule {
  private final List<LootableDefinition> lootableDefinitions = new ArrayList<>();

  @Override
  public MatchModule createMatchModule(Match match) {
    return new LootMatchModule(match, lootableDefinitions);
  }

  public static class Factory implements MapModuleFactory<LootModule> {
    @Override
    public LootModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      LootModule lootModule = new LootModule();
      FilterParser filterParser = factory.getFilters();
      KitParser kitParser = factory.getKits();
      RegionParser regionParser = factory.getRegions();
      AtomicInteger lootableIdSerial = new AtomicInteger(1);
      AtomicInteger cacheIdSerial = new AtomicInteger(1);

      Element lootablesElement = doc.getRootElement().getChild("lootables");
      if (lootablesElement != null) {
        for (Element lootEl : lootablesElement.getChildren("loot")) {
          String id = lootEl.getAttributeValue("id");
          if (id == null) {
            id = LootableDefinition.makeDefaultId("lootable", lootableIdSerial);
          }
          List<Loot> lootables = new ArrayList<>();
          parseAndAddItems(lootEl, lootables, kitParser, id);

          List<Any> anyLootables = new ArrayList<>();
          for (Element anyEl : lootEl.getChildren("any")) {
            int count =
                XMLUtils.parseNumberInRange(
                    Node.fromAttr(anyEl, "count"), Integer.class, Range.atLeast(1), 1);
            boolean unique = XMLUtils.parseBoolean(anyEl.getAttribute("unique"), true);
            List<Loot> anyItems = new ArrayList<>();
            parseAndAddItems(anyEl, anyItems, kitParser, id);

            // items inside <option>
            List<Element> optionsEl = anyEl.getChildren("option");
            List<Option> options = new ArrayList<>();
            for (Element optionEl : optionsEl) {
              int weight = XMLUtils.parseNumber(optionEl, int.class, 1);
              Filter filter = filterParser.parseFilterProperty(optionEl, "filter");
              ItemStack stack = kitParser.parseItem(optionEl, false);
              Loot item = new Loot(stack, id);
              options.add(new Option(weight, filter, item));
            }
            if (!options.isEmpty() && !anyItems.isEmpty()) {
              throw new InvalidXMLException("all <any> children must contain <option>", anyEl);
            }
            anyLootables.add(new Any(anyItems, options, count, unique));
          }

          List<Maybe> maybeLootables = new ArrayList<>();
          for (Element maybeEl : lootEl.getChildren("maybe")) {
            Filter filter = filterParser.parseRequiredFilterProperty(maybeEl, "filter");
            List<Loot> maybeItems = new ArrayList<>();
            parseAndAddItems(maybeEl, maybeItems, kitParser, id);
            maybeLootables.add(new Maybe(maybeItems, filter));
          }

          Filter filter = null;
          Duration refillInterval = null;
          boolean refillClear = true;
          Element fillEl = lootEl.getChild("fill");
          // <fill> outside <loot>, legacy
          if (fillEl == null) {
            List<Element> fillListEl = lootablesElement.getChildren("fill");
            if (fillListEl != null) {
              for (Element listEl : fillListEl) {
                if (listEl.getAttributeValue("loot").equals(id)) {
                  filter = filterParser.parseFilterProperty(listEl, "filter", StaticFilter.ALLOW);
                  // TODO add dynamic filter refill-trigger
                  // default to infinite duration
                  refillInterval =
                      XMLUtils.parseDuration(listEl.getAttribute("refill-interval"), null);
                  refillClear = XMLUtils.parseBoolean(listEl.getAttribute("refill-clear"), true);
                }
              }
            } else {
              throw new InvalidXMLException("<loot> requires child <fill> element", lootEl);
            }
            // <fill> inside <loot>
          } else {
            filter = filterParser.parseFilterProperty(fillEl, "filter", StaticFilter.ALLOW);
            // TODO add dynamic filter refill-trigger
            // default to infinite duration
            refillInterval = XMLUtils.parseDuration(fillEl.getAttribute("refill-interval"), null);
            refillClear = XMLUtils.parseBoolean(fillEl.getAttribute("refill-clear"), true);
          }
          LootableDefinition lootableDefinition =
              new LootableDefinition(
                  id,
                  lootables,
                  anyLootables,
                  maybeLootables,
                  null,
                  filter,
                  refillInterval,
                  refillClear);
          factory.getFeatures().addFeature(lootEl, lootableDefinition);
          lootModule.lootableDefinitions.add(lootableDefinition);
        }
        List<Cache> caches = new ArrayList<>();
        for (Element cacheEl : lootablesElement.getChildren("cache")) {
          Filter cacheFilter =
              filterParser.parseFilterProperty(cacheEl, "filter", StaticFilter.ALLOW);
          Region region = regionParser.parseRegionProperty(cacheEl, "region");
          caches.add(new Cache(cacheFilter, region));
        }
        for (Cache cache : caches) {
          String id = LootableDefinition.makeDefaultId("lootable-cache", cacheIdSerial);
          LootableDefinition lootableDefinition =
              new LootableDefinition(id, null, null, null, cache, null, null, false);
          factory.getFeatures().addFeature(lootablesElement, lootableDefinition);
          lootModule.lootableDefinitions.add(lootableDefinition);
        }
      }
      return lootModule;
    }

    public void parseAndAddItems(Element el, List<Loot> lootList, KitParser kitParser, String id)
        throws InvalidXMLException {
      for (Element itemEl : XMLUtils.getChildren(el, "item")) {
        ItemStack stack = kitParser.parseItem(itemEl, false);
        Loot item = new Loot(stack, id);
        lootList.add(item);
      }
    }
  }
}

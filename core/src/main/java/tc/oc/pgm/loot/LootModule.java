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
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.kits.KitParser;
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
      AtomicInteger lootableIdSerial = new AtomicInteger(1);
      Element lootablesElement = doc.getRootElement().getChild("lootables");
      for (Element lootElement : lootablesElement.getChildren("loot")) {
        String id = lootElement.getAttributeValue("id");
        if (id == null) {
          id = LootableDefinition.makeDefaultId(null, lootableIdSerial);
        }
        List<Loot> lootables = new ArrayList<>();
        parseAndAddItems(lootElement, lootables, kitParser, id);
        List<Any> anyLootables = new ArrayList<>();
        for (Element anyElement : lootElement.getChildren("any")) {
          int count =
              XMLUtils.parseNumberInRange(
                  Node.fromAttr(anyElement, "count"), Integer.class, Range.atLeast(1), 1);
          boolean unique = XMLUtils.parseBoolean(anyElement, true);
          List<Loot> anyItems = new ArrayList<>();
          parseAndAddItems(anyElement, anyItems, kitParser, id);
          anyLootables.add(new Any(anyItems, count, unique));
          // TODO readd <option>
        }
        List<Maybe> maybeLootables = new ArrayList<>();
        for (Element maybeElement : lootElement.getChildren("maybe")) {
          Filter filter = filterParser.parseRequiredFilterProperty(maybeElement, "filter");
          List<Loot> maybeItems = new ArrayList<>();
          parseAndAddItems(maybeElement, maybeItems, kitParser, id);
          maybeLootables.add(new Maybe(maybeItems, filter));
        }
        Filter filter = null;
        Duration refillInterval = null;
        boolean refillClear = true;
        Element fillElement = lootElement.getChild("fill");
        // <fill> outside <loot>, legacy
        if (fillElement == null) {
          List<Element> fillListEl = lootablesElement.getChildren("fill");
          if (fillListEl != null) {
            for (Element listEl : fillListEl) {
              if (listEl.getAttributeValue("id").equals(id)) {
                filter = filterParser.parseFilterProperty(listEl, "filter", StaticFilter.ALLOW);
                // TODO add dynamic filter refill-trigger
                // default to infinite duration
                refillInterval =
                    XMLUtils.parseDuration(listEl.getAttribute("refill-interval"), null);
                refillClear = XMLUtils.parseBoolean(listEl.getAttribute("refill-clear"), true);
              }
            }
          } else {
            throw new InvalidXMLException("<loot> requires child <fill> element", lootElement);
          }
          // <fill> inside <loot>
        } else {
          filter = filterParser.parseFilterProperty(fillElement, "filter", StaticFilter.ALLOW);
          // TODO add dynamic filter refill-trigger
          // default to infinite duration
          refillInterval =
              XMLUtils.parseDuration(fillElement.getAttribute("refill-interval"), null);
          refillClear = XMLUtils.parseBoolean(fillElement.getAttribute("refill-clear"), true);
        }
        LootableDefinition lootableDefinition =
            new LootableDefinition(
                id, lootables, anyLootables, maybeLootables, filter, refillInterval, refillClear);
        factory.getFeatures().addFeature(lootElement, lootableDefinition);
        lootModule.lootableDefinitions.add(lootableDefinition);
      }
      return null;
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

package tc.oc.pgm.modules;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.matcher.block.BlockFilter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ItemKeepModule implements MapModule<ItemKeepMatchModule> {
  protected final Set<BlockFilter> itemsToKeep;
  protected final Set<BlockFilter> armorToKeep;

  public ItemKeepModule(Set<BlockFilter> itemsToKeep, Set<BlockFilter> armorToKeep) {
    this.itemsToKeep = itemsToKeep;
    this.armorToKeep = armorToKeep;
  }

  @Override
  public ItemKeepMatchModule createMatchModule(Match match) {
    return new ItemKeepMatchModule(match, this.itemsToKeep, this.armorToKeep);
  }

  public static class Factory implements MapModuleFactory<ItemKeepModule> {
    @Override
    public ItemKeepModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<BlockFilter> itemsToKeep = Sets.newHashSet();
      for (Node elItemKeep : Node.fromChildren(doc.getRootElement(), "item-keep", "itemkeep")) {
        for (Node elItem : Node.fromChildren(elItemKeep.getElement(), "item")) {
          itemsToKeep.add(new BlockFilter(XMLUtils.parseMaterialPattern(elItem)));
        }
      }

      Set<BlockFilter> armorToKeep = Sets.newHashSet();
      for (Node elArmorKeep : Node.fromChildren(doc.getRootElement(), "armor-keep", "armorkeep")) {
        for (Node elItem : Node.fromChildren(elArmorKeep.getElement(), "item")) {
          armorToKeep.add(new BlockFilter(XMLUtils.parseMaterialPattern(elItem)));
        }
      }

      if (itemsToKeep.isEmpty() && armorToKeep.isEmpty()) {
        return null;
      } else {
        return new ItemKeepModule(itemsToKeep, armorToKeep);
      }
    }
  }
}

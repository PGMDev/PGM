package tc.oc.pgm.modules;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.BlockFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "Item keep")
public class ItemKeepModule extends MapModule<ItemKeepMatchModule> {
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

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static ItemKeepModule parse(MapModuleContext context, Logger logger, Document doc)
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

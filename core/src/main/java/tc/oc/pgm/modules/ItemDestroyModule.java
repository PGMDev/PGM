package tc.oc.pgm.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.matcher.block.MaterialFilter;
import tc.oc.pgm.util.material.matcher.CompoundMaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ItemDestroyModule implements MapModule<ItemDestroyMatchModule> {
  protected final Filter itemsToRemove;

  public ItemDestroyModule(Filter itemsToRemove) {
    this.itemsToRemove = itemsToRemove;
  }

  @Override
  public ItemDestroyMatchModule createMatchModule(Match match) {
    return new ItemDestroyMatchModule(match, this.itemsToRemove);
  }

  public static class Factory implements MapModuleFactory<ItemDestroyModule> {
    @Override
    public ItemDestroyModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<SingleMaterialMatcher> itemsToRemove = new ArrayList<>();
      for (Node itemRemoveNode :
          Node.fromChildren(doc.getRootElement(), "item-remove", "itemremove")) {
        for (Node itemNode : Node.fromChildren(itemRemoveNode.getElement(), "item")) {
          itemsToRemove.add(XMLUtils.parseMaterialPattern(itemNode));
        }
      }
      if (itemsToRemove.isEmpty()) {
        return null;
      } else {
        return new ItemDestroyModule(new MaterialFilter(CompoundMaterialMatcher.of(itemsToRemove)));
      }
    }
  }
}

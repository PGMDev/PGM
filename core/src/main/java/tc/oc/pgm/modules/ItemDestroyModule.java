package tc.oc.pgm.modules;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.matcher.block.MaterialFilter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ItemDestroyModule implements MapModule<ItemDestroyMatchModule> {
  protected final Set<Filter> itemsToRemove;

  public ItemDestroyModule(Set<Filter> itemsToRemove) {
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
      Set<Filter> itemsToRemove = Sets.newHashSet();
      for (Node itemRemoveNode :
          Node.fromChildren(doc.getRootElement(), "item-remove", "itemremove")) {
        for (Node itemNode : Node.fromChildren(itemRemoveNode.getElement(), "item")) {
          itemsToRemove.add(new MaterialFilter(XMLUtils.parseMaterialPattern(itemNode)));
        }
      }
      if (itemsToRemove.isEmpty()) {
        return null;
      } else {
        return new ItemDestroyModule(itemsToRemove);
      }
    }
  }
}

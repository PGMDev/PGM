package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class ItemDestroyModule implements MapModule<ItemDestroyMatchModule> {
  protected final MaterialMatcher itemsToRemove;

  public ItemDestroyModule(MaterialMatcher itemsToRemove) {
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
      MaterialMatcher.Builder itemsToRemove = MaterialMatcher.builder();
      for (Node itemRemoveNode :
          Node.fromChildren(doc.getRootElement(), "item-remove", "itemremove")) {
        for (Node itemNode : Node.fromChildren(itemRemoveNode.getElement(), "item")) {
          itemsToRemove.parse(itemNode);
        }
      }
      if (itemsToRemove.isEmpty()) {
        return null;
      } else {
        return new ItemDestroyModule(itemsToRemove.build());
      }
    }
  }
}

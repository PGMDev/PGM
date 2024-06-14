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

public class ToolRepairModule implements MapModule<ToolRepairMatchModule> {
  protected final MaterialMatcher toRepair;

  public ToolRepairModule(MaterialMatcher toRepair) {
    this.toRepair = toRepair;
  }

  @Override
  public ToolRepairMatchModule createMatchModule(Match match) {
    return new ToolRepairMatchModule(match, this.toRepair);
  }

  public static class Factory implements MapModuleFactory<ToolRepairModule> {
    @Override
    public ToolRepairModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      MaterialMatcher.Builder toRepair = MaterialMatcher.builder().materialsOnly();
      for (Node toolRepairElement :
          Node.fromChildren(doc.getRootElement(), "tool-repair", "toolrepair")) {
        for (Node toolElement : Node.fromChildren(toolRepairElement.getElement(), "tool")) {
          toRepair.parse(toolElement);
        }
      }
      if (toRepair.isEmpty()) {
        return null;
      } else {
        return new ToolRepairModule(toRepair.build());
      }
    }
  }
}

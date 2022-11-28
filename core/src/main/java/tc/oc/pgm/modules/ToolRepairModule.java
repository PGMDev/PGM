package tc.oc.pgm.modules;

import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ToolRepairModule implements MapModule<ToolRepairMatchModule> {
  protected final Set<Material> toRepair;

  public ToolRepairModule(Set<Material> toRepair) {
    this.toRepair = ImmutableSet.copyOf(toRepair);
  }

  @Override
  public ToolRepairMatchModule createMatchModule(Match match) {
    return new ToolRepairMatchModule(match, this.toRepair);
  }

  public static class Factory implements MapModuleFactory<ToolRepairModule> {
    @Override
    public ToolRepairModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<Material> toRepair = EnumSet.noneOf(Material.class);
      for (Node toolRepairElement :
          Node.fromChildren(doc.getRootElement(), "tool-repair", "toolrepair")) {
        for (Node toolElement : Node.fromChildren(toolRepairElement.getElement(), "tool")) {
          toRepair.add(XMLUtils.parseMaterial(toolElement));
        }
      }
      if (toRepair.size() == 0) {
        return null;
      } else {
        return new ToolRepairModule(toRepair);
      }
    }
  }
}

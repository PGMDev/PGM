package tc.oc.pgm.kits;

import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.FeatureDefinition;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Kit",
    requires = {InfoModule.class})
public class KitModule extends MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new KitMatchModule(match);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static KitModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    for (Element kitsElement : doc.getRootElement().getChildren("kits")) {
      for (Element kitElement : kitsElement.getChildren("kit")) {
        context.getKitParser().parse(kitElement);
      }
    }
    return new KitModule();
  }

  @Override
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    ItemModifyModule imm = context.getModule(ItemModifyModule.class);
    for (Kit kit : context.getKitParser().getKits()) {
      if (kit instanceof RemoveKit && !((RemoveKit) kit).getKit().isRemovable()) {
        throw new InvalidXMLException(
            "kit is not removable", context.features().getNode((FeatureDefinition) kit));
      }

      // Apply any item-mods rules to item kits
      if (imm != null) {
        if (kit instanceof ItemKit) {
          for (ItemStack stack : ((ItemKit) kit).getSlotItems().values()) {
            imm.applyRules(stack);
          }
        }

        if (kit instanceof ArmorKit) {
          for (ArmorKit.ArmorItem armor : ((ArmorKit) kit).getArmor().values()) {
            imm.applyRules(armor.stack);
          }
        }
      }
    }
  }
}

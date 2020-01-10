package tc.oc.pgm.kits;

import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.features.FeatureDefinition;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.xml.InvalidXMLException;

public class KitModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new KitMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<KitModule> {
    @Override
    public KitModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      for (Element kitsElement : doc.getRootElement().getChildren("kits")) {
        for (Element kitElement : kitsElement.getChildren("kit")) {
          context.legacy().getKits().parse(kitElement);
        }
      }
      return new KitModule();
    }
  }

  @Override
  public void postParse(MapContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    ItemModifyModule imm = context.getModule(ItemModifyModule.class);
    for (Kit kit : context.legacy().getKits().getKits()) {
      if (kit instanceof RemoveKit && !((RemoveKit) kit).getKit().isRemovable()) {
        throw new InvalidXMLException(
            "kit is not removable",
            context.legacy().getFeatures().getNode((FeatureDefinition) kit));
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

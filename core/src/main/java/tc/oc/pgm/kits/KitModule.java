package tc.oc.pgm.kits;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class KitModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new KitMatchModule(match);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getWeakDependencies() {
    return ImmutableList.of(TeamMatchModule.class);
  }

  public static class Factory implements MapModuleFactory<KitModule> {

    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

    @Override
    public KitModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      for (Element kitsElement : doc.getRootElement().getChildren("kits")) {
        for (Element kitElement : kitsElement.getChildren("kit")) {
          factory.getKits().parse(kitElement);
        }
      }
      return new KitModule();
    }
  }

  @Override
  public void postParse(MapFactory factory, Logger logger, Document doc)
      throws InvalidXMLException {
    ItemModifyModule imm = factory.getModule(ItemModifyModule.class);
    for (Kit kit : factory.getKits().getKits()) {
      if (kit instanceof RemoveKit && !((RemoveKit) kit).getKit().isRemovable()) {
        throw new InvalidXMLException(
            "kit is not removable", factory.getFeatures().getNode((FeatureDefinition) kit));
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

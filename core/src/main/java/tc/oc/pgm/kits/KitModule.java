package tc.oc.pgm.kits;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class KitModule implements MapModule {

  protected final Set<KitRule> kitRules;
  protected final List<Kit> kits;

  public KitModule(Set<KitRule> kitRules, List<Kit> kits) {
    this.kitRules = ImmutableSet.copyOf(kitRules);
    this.kits = kits;
  }

  public Set<KitRule> getKitRules() {
    return kitRules;
  }

  public List<Kit> getKits() {
    return kits;
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new KitMatchModule(match, this);
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
      Set<KitRule> kitRules = Sets.newHashSet();
      List<Kit> kits = new ArrayList<Kit>();

      for (Element kitsElement : doc.getRootElement().getChildren("kits")) {
        for (Element kitElement : kitsElement.getChildren("kit")) {
          kits.add(factory.getKits().parse(kitElement));
        }
        for (Element kitElement : XMLUtils.getChildren(kitsElement, "give", "take", "lend")) {
          KitRule kitRule = parseRule(factory, kitElement);
          kitRules.add(kitRule);
          factory.getFeatures().addFeature(kitElement, kitRule);
        }
      }

      return new KitModule(kitRules, kits);
    }

    private KitRule parseRule(MapFactory factory, Element el) throws InvalidXMLException {
      KitRule.Action action = TextParser.parseEnum(el.getName(), KitRule.Action.class);
      Kit kit = factory.getKits().parseKitProperty(el, "kit");
      Filter filter = factory.getFilters().parseFilterProperty(el, "filter");

      return new KitRule(action, kit, filter);
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

    for (KitRule kitRule : this.kitRules) {
      if ((kitRule.getAction() == KitRule.Action.TAKE || kitRule.getAction() == KitRule.Action.LEND)
          && !kitRule.getKit().isRemovable()) {
        throw new InvalidXMLException(
            "kit is not removable", factory.getFeatures().getNode(kitRule));
      }
    }
  }
}

package tc.oc.pgm.blockdrops;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class BlockDropsModule implements MapModule<BlockDropsMatchModule> {
  private final BlockDropsRuleSet ruleSet;

  public BlockDropsModule(BlockDropsRuleSet ruleSet) {
    this.ruleSet = ruleSet;
  }

  @Override
  public BlockDropsMatchModule createMatchModule(Match match) {
    return new BlockDropsMatchModule(match, this.ruleSet);
  }

  public static class Factory implements MapModuleFactory<BlockDropsModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(
          KitModule.class, RegionModule.class, FilterModule.class, ItemModifyModule.class);
    }

    @Override
    public BlockDropsModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<BlockDropsRule> rules = new ArrayList<>();
      FilterParser filterParser = factory.getFilters();
      RegionParser regionParser = factory.getRegions();
      KitParser kitParser = factory.getKits();
      final Optional<ItemModifyModule> itemModifier =
          Optional.ofNullable(factory.getModule(ItemModifyModule.class));

      for (Element elRule :
          XMLUtils.flattenElements(
              doc.getRootElement(),
              ImmutableSet.of("block-drops", "blockdrops"),
              ImmutableSet.of("rule"))) {
        Filter filter = filterParser.parseFilterProperty(elRule, "filter");
        Region region = regionParser.parseRegionProperty(elRule, "region");
        Kit kit = kitParser.parseKitProperty(elRule, "kit", null);

        boolean dropOnWrongTool =
            XMLUtils.parseBoolean(Node.fromChildOrAttr(elRule, "wrong-tool", "wrongtool"), false);
        boolean punchable = XMLUtils.parseBoolean(Node.fromChildOrAttr(elRule, "punch"), false);
        boolean trample = XMLUtils.parseBoolean(Node.fromChildOrAttr(elRule, "trample"), false);
        Float fallChance =
            XMLUtils.parseNumber(Node.fromChildOrAttr(elRule, "fall-chance"), Float.class, null);
        Float landChance =
            XMLUtils.parseNumber(Node.fromChildOrAttr(elRule, "land-chance"), Float.class, null);
        Double fallSpeed =
            XMLUtils.parseNumber(Node.fromChildOrAttr(elRule, "fall-speed"), Double.class, null);

        MaterialData replacement = null;
        if (elRule.getChild("replacement") != null) {
          replacement =
              XMLUtils.parseBlockMaterialData(Node.fromChildOrAttr(elRule, "replacement"));
        }

        int experience =
            XMLUtils.parseNumber(Node.fromChildOrAttr(elRule, "experience"), Integer.class, 0);

        Map<ItemStack, Double> items = new LinkedHashMap<>();
        for (Element elDrops : elRule.getChildren("drops")) {
          for (Element elItem : elDrops.getChildren("item")) {
            final ItemStack itemStack = factory.getKits().parseItem(elItem, false);
            itemModifier.ifPresent(imm -> imm.applyRules(itemStack));
            items.put(
                itemStack, XMLUtils.parseNumber(elItem.getAttribute("chance"), Double.class, 1d));
          }
        }

        rules.add(
            new BlockDropsRule(
                filter,
                region,
                dropOnWrongTool,
                punchable,
                trample,
                new BlockDrops(
                    items, kit, experience, replacement, fallChance, landChance, fallSpeed)));
      }

      return rules.isEmpty() ? null : new BlockDropsModule(new BlockDropsRuleSet(rules));
    }
  }

  @Override
  public void postParse(MapFactory factory, Logger logger, Document doc)
      throws InvalidXMLException {
    // Apply any item-mods to all drops
    ItemModifyModule imm = factory.getModule(ItemModifyModule.class);
    if (imm != null) {
      for (BlockDropsRule rule : ruleSet.getRules()) {
        for (Map.Entry<ItemStack, Double> entry : rule.drops.items.entrySet()) {
          imm.applyRules(entry.getKey());
        }
      }
    }
  }
}

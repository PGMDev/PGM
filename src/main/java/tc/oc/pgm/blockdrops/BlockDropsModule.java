package tc.oc.pgm.blockdrops;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.itemmeta.ItemModifyModule;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.Pair;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(
    name = "Custom Block Drops",
    requires = {InfoModule.class},
    follows = {KitModule.class, RegionModule.class, FilterModule.class})
public class BlockDropsModule extends MapModule<BlockDropsMatchModule> {
  private final BlockDropsRuleSet ruleSet;

  public BlockDropsModule(BlockDropsRuleSet ruleSet) {
    this.ruleSet = ruleSet;
  }

  @Override
  public BlockDropsMatchModule createMatchModule(Match match) {
    return new BlockDropsMatchModule(match, this.ruleSet);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------
  @SuppressWarnings("deprecation")
  public static BlockDropsModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<BlockDropsRule> rules = new ArrayList<>();
    FilterParser filterParser = context.getFilterParser();
    RegionParser regionParser = context.getRegionParser();

    for (Element elRule :
        XMLUtils.flattenElements(
            doc.getRootElement(),
            ImmutableSet.of("block-drops", "blockdrops"),
            ImmutableSet.of("rule"))) {
      Filter filter = filterParser.parseFilterProperty(elRule, "filter");
      Region region = regionParser.parseRegionProperty(elRule, "region");

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
        replacement = XMLUtils.parseBlockMaterialData(Node.fromChildOrAttr(elRule, "replacement"));
      }

      int experience =
          XMLUtils.parseNumber(Node.fromChildOrAttr(elRule, "experience"), Integer.class, 0);

      List<Pair<Double, ItemStack>> items = new ArrayList<>();
      for (Element elDrops : elRule.getChildren("drops")) {
        for (Element elItem : elDrops.getChildren("item")) {
          items.add(
              Pair.create(
                  XMLUtils.parseNumber(elItem.getAttribute("chance"), Double.class, 1d),
                  context.getKitParser().parseItem(elItem, false)));
        }
      }

      rules.add(
          new BlockDropsRule(
              filter,
              region,
              dropOnWrongTool,
              punchable,
              trample,
              new BlockDrops(items, experience, replacement, fallChance, landChance, fallSpeed)));
    }

    // BlockDropsModule must always be loaded, even if there are no rules defined,
    // otherwise modules that depend on it e.g. DestroyablesModule will be silently
    // skipped by the module loader. We need better module dependency logic.
    return new BlockDropsModule(new BlockDropsRuleSet(rules));
  }

  @Override
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    // Apply any item-mods to all drops
    ItemModifyModule imm = context.getModule(ItemModifyModule.class);
    if (imm != null) {
      for (BlockDropsRule rule : ruleSet.getRules()) {
        for (Pair<Double, ItemStack> entry : rule.drops.items) {
          imm.applyRules(entry.second);
        }
      }
    }
  }
}

package tc.oc.pgm.itemmeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ItemModifyModule implements MapModule<ItemModifyMatchModule> {
  private static final ItemTag<Boolean> APPLIED = ItemTag.newBoolean("custom-meta-applied");
  private final List<ItemRule> rules;

  public ItemModifyModule(List<ItemRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public boolean applyRules(ItemStack stack) {
    if (rules.isEmpty() || stack == null || stack.getType() == Material.AIR || APPLIED.has(stack)) {
      return false;
    } else {
      for (ItemRule rule : rules) {
        if (rule.matches(stack)) {
          rule.apply(stack);
          APPLIED.set(stack, true);
        }
      }
      return true;
    }
  }

  @Override
  public ItemModifyMatchModule createMatchModule(Match match) {
    return rules.isEmpty() ? null : new ItemModifyMatchModule(match, this);
  }

  public static class Factory implements MapModuleFactory<ItemModifyModule> {
    @Override
    public @Nullable ItemModifyModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();

      List<ItemRule> rules = new ArrayList<>();
      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "item-mods", "rule")) {
        var items =
            parser.node(XMLUtils::parseMaterialMatcher, el, "match").child().required();
        var material = items.getMaterials().iterator().next();

        var elModify = XMLUtils.getRequiredUniqueChild(el, "modify");
        var applicator = factory.getKits().parseItemMeta(material, elModify, true);

        ItemRule rule = new ItemRule(items, applicator);
        rules.add(rule);
      }

      return new ItemModifyModule(rules);
    }
  }
}

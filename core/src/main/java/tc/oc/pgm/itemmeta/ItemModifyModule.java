package tc.oc.pgm.itemmeta;

import static tc.oc.pgm.util.inventory.InventoryUtils.INVENTORY_UTILS;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.bukkit.ComponentApplicator;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record ItemModifyModule(List<ItemRule> rules) implements MapModule<ItemModifyMatchModule> {
  private static final ItemTag<Boolean> APPLIED = ItemTag.newBoolean("custom-meta-applied");

  public boolean applyRules(ItemStack stack) {
    if (stack == null || stack.getType() == Material.AIR || APPLIED.has(stack)) {
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
    return new ItemModifyMatchModule(match, this);
  }

  public static class Factory implements MapModuleFactory<ItemModifyModule> {
    @Override
    public @Nullable ItemModifyModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<ItemRule> rules = new ArrayList<>();
      for (Element elRule : XMLUtils.flattenElements(doc.getRootElement(), "item-mods", "rule")) {
        MaterialMatcher items =
            XMLUtils.parseMaterialMatcher(XMLUtils.getRequiredUniqueChild(elRule, "match"));

        // Always use a PotionMeta so the rule can have potion effects, though it will only apply
        // those to potion items
        Element elModify = XMLUtils.getRequiredUniqueChild(elRule, "modify");
        PotionMeta meta = (PotionMeta) Bukkit.getItemFactory().getItemMeta(Material.POTION);
        factory.getKits().parseItemMeta(elModify, meta);

        ComponentApplicator applicator = null;
        Node components = Node.fromChildOrAttr(elModify, "components");
        if (components != null) applicator = INVENTORY_UTILS.buildComponentApplicator(components);

        ItemRule rule = new ItemRule(items, meta, applicator);
        rules.add(rule);
      }

      return rules.isEmpty() ? null : new ItemModifyModule(rules);
    }
  }
}

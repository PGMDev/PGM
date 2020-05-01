package tc.oc.pgm.itemmeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ItemModifyModule implements MapModule<ItemModifyMatchModule> {
  private static final ItemTag<Boolean> APPLIED = ItemTag.newBoolean("custom-meta-applied");
  private final List<ItemRule> rules;

  public ItemModifyModule(List<ItemRule> rules) {
    this.rules = rules;
  }

  public boolean applyRules(ItemStack stack) {
    if (stack == null || stack.getType() == Material.AIR || APPLIED.has(stack)) {
      return false;
    } else {
      APPLIED.set(stack, true);
      for (ItemRule rule : rules) {
        if (rule.matches(stack)) {
          rule.apply(stack);
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
        PotionMeta meta = (PotionMeta) Bukkit.getItemFactory().getItemMeta(Material.POTION);
        factory.getKits().parseItemMeta(XMLUtils.getRequiredUniqueChild(elRule, "modify"), meta);

        ItemRule rule = new ItemRule(items, meta);
        rules.add(rule);
      }

      return rules.isEmpty() ? null : new ItemModifyModule(rules);
    }
  }
}

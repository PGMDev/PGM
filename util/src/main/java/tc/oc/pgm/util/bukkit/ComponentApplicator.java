package tc.oc.pgm.util.bukkit;

import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.material.MaterialMatcher;

@FunctionalInterface
public interface ComponentApplicator {
  ComponentApplicator EMPTY = is -> {};

  void apply(ItemStack stack);

  default ComponentApplicator andThen(ComponentApplicator other) {
    if (other == EMPTY) return this;
    if (this == EMPTY) return other;

    record MultiApplicator(List<ComponentApplicator> applicators) implements ComponentApplicator {
      @Override
      public void apply(ItemStack stack) {
        applicators.forEach(applicator -> applicator.apply(stack));
      }

      @Override
      public ComponentApplicator andThen(ComponentApplicator other) {
        if (other == EMPTY) return this;
        List<ComponentApplicator> combined = new ArrayList<>(applicators);
        if (other instanceof MultiApplicator(var list)) combined.addAll(list);
        else combined.add(other);
        return new MultiApplicator(List.copyOf(combined));
      }
    }
    return new MultiApplicator(List.of(this, other));
  }

  interface Builder {
    void addEnchantments(Map<Enchantment, Integer> enchants);

    void addStoredEnchantments(Map<Enchantment, Integer> enchants);

    void addPotions(List<PotionEffect> effects);

    void addAttributeModifiers(SetMultimap<Attribute, AttributeModifier> attributeModifiers);

    void addDisplayName(String name);

    void addColor(Color color);

    void addLore(List<String> lore);

    void addItemFlags(ItemFlag... flags);

    void addUnbreakable();

    void addCanDestroy(MaterialMatcher materials);

    void addCanPlaceOn(MaterialMatcher materials);

    // Custom components on modern can go here
    void addComponents(ComponentApplicator components);

    ComponentApplicator build();
  }
}

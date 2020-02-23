package tc.oc.util.bukkit.item;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.ImmutableMaterialSet;

public interface Items {

  static void addEnchantments(ItemMeta meta, Map<Enchantment, Integer> enchantments) {
    for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
      if (meta.getEnchantLevel(enchantment.getKey()) < enchantment.getValue()) {
        meta.addEnchant(enchantment.getKey(), enchantment.getValue(), true);
      }
    }
  }

  static void addPotionEffects(ItemStack stack, List<PotionEffect> newEffects) {
    if (stack.getType() == Material.POTION && !newEffects.isEmpty()) {
      PotionMeta meta = (PotionMeta) stack.getItemMeta();

      Set<PotionEffect> defaultEffects = new HashSet<>(Potion.fromItemStack(stack).getEffects());
      Collection<PotionEffect> existingEffects;

      if (meta.hasCustomEffects()) {
        existingEffects = meta.getCustomEffects();
      } else {
        existingEffects = defaultEffects;
      }

      Map<PotionEffectType, PotionEffect> effectsByType = new HashMap<>();
      for (PotionEffect effect : existingEffects) {
        effectsByType.put(effect.getType(), effect);
      }

      for (PotionEffect newEffect : newEffects) {
        PotionEffect oldEffect = effectsByType.get(newEffect.getType());
        if (oldEffect == null
            || oldEffect.getAmplifier() < newEffect.getAmplifier()
            || (oldEffect.getAmplifier() == newEffect.getAmplifier()
                && oldEffect.getDuration() < newEffect.getDuration())) {

          effectsByType.put(newEffect.getType(), newEffect);
        }
      }

      if (defaultEffects.equals(ImmutableSet.copyOf(effectsByType.values()))) {
        meta.clearCustomEffects();
      } else {
        for (PotionEffect effect : effectsByType.values()) {
          meta.addCustomEffect(effect, true);
        }
      }

      stack.setItemMeta(meta);
    }
  }

  static ImmutableMaterialSet unionMaterials(ImmutableMaterialSet a, ImmutableMaterialSet b) {
    if (a.containsAll(b)) return a;
    if (b.containsAll(a)) return b;
    return ImmutableMaterialSet.of(Sets.union(a, b));
  }

  static boolean isNothing(ItemStack stack) {
    return stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0;
  }

  static ItemStack placeStack(Inventory inv, int slot, ItemStack stack) {
    ItemStack placed = inv.getItem(slot);
    int max = Math.min(inv.getMaxStackSize(), stack.getMaxStackSize());
    int amount;
    int leftover;

    if (Items.isNothing(placed)) {
      amount = Math.min(max, stack.getAmount());
      leftover = Math.max(0, stack.getAmount() - amount);
    } else if (placed.isSimilar(stack)) {
      amount = Math.min(max, placed.getAmount() + stack.getAmount());
      leftover = placed.getAmount() + stack.getAmount() - amount;
    } else {
      return stack;
    }

    if (leftover == stack.getAmount()) {
      return stack;
    } else {
      placed = stack.clone();
      placed.setAmount(amount);
      inv.setItem(slot, placed);

      stack = stack.clone();
      stack.setAmount(leftover);
      return stack;
    }
  }

  static ItemStack placeStack(Inventory inv, Iterable<Integer> slots, ItemStack stack) {
    for (int slot : slots) {
      ItemStack existing = inv.getItem(slot);
      if (existing != null && existing.isSimilar(stack)) {
        stack = placeStack(inv, slot, stack);
        if (stack.getAmount() == 0) return stack;
      }
    }

    for (int slot : slots) {
      if (Items.isNothing(inv.getItem(slot))) {
        stack = placeStack(inv, slot, stack);
        if (stack.getAmount() == 0) return stack;
      }
    }

    return stack;
  }
}

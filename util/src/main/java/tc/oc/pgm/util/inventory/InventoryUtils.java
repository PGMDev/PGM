package tc.oc.pgm.util.inventory;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public final class InventoryUtils {
  private InventoryUtils() {}

  public static boolean isNothing(ItemStack stack) {
    return stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0;
  }

  public static void addEnchantments(ItemMeta meta, Map<Enchantment, Integer> enchantments) {
    for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
      if (meta.getEnchantLevel(enchantment.getKey()) < enchantment.getValue()) {
        meta.addEnchant(enchantment.getKey(), enchantment.getValue(), true);
      }
    }
  }

  public static void addEffects(ItemStack stack, List<PotionEffect> newEffects) {
    if (stack.getType() != Material.POTION || newEffects.isEmpty()) return;
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

  public static Collection<PotionEffect> getEffects(ItemStack potion) {
    if (potion.getItemMeta() instanceof PotionMeta) {
      PotionMeta meta = (PotionMeta) potion.getItemMeta();
      if (meta.hasCustomEffects()) {
        return meta.getCustomEffects();
      } else {
        return Potion.fromItemStack(potion).getEffects();
      }
    } else {
      return Collections.emptyList();
    }
  }

  public static @Nullable PotionEffectType getPrimaryEffectType(ItemStack potion) {
    for (PotionEffect effect : getEffects(potion)) {
      return effect.getType();
    }
    return null;
  }

  public static ItemStack placeStack(Inventory inv, int slot, ItemStack stack) {
    ItemStack placed = inv.getItem(slot);
    int max = Math.min(inv.getMaxStackSize(), stack.getMaxStackSize());
    int amount;
    int leftover;

    if (isNothing(placed)) {
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

  public static ItemStack placeStack(Inventory inv, Iterable<Integer> slots, ItemStack stack) {
    for (int slot : slots) {
      ItemStack existing = inv.getItem(slot);
      if (existing != null && existing.isSimilar(stack)) {
        stack = placeStack(inv, slot, stack);
        if (stack.getAmount() == 0) return stack;
      }
    }

    for (int slot : slots) {
      if (isNothing(inv.getItem(slot))) {
        stack = placeStack(inv, slot, stack);
        if (stack.getAmount() == 0) return stack;
      }
    }

    return stack;
  }

  public static void openVillager(Villager villager, Player viewer) throws Throwable {
    // An exception can be thrown if the Villager's NBT is invalid
    // or if the server does not support for this patch.
    // TODO: Newer versions of Bukkit can use HumanEntity#openMerchant(Merchant, boolean)
    viewer.openMerchantCopy(villager);
  }

  public static void consumeItem(Player player) {
    ItemStack itemInHand = player.getItemInHand();
    if (itemInHand.getAmount() > 1) {
      itemInHand.setAmount(itemInHand.getAmount() - 1);
    } else {
      player.setItemInHand(null);
    }
  }
}

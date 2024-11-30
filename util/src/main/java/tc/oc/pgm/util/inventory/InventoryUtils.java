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
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.platform.Platform;

public final class InventoryUtils {
  public static final InventoryUtilsPlatform INVENTORY_UTILS =
      Platform.get(InventoryUtilsPlatform.class);

  public static final ItemFlag HIDE_ADDITIONAL_FLAG =
      BukkitUtils.parse(ItemFlag::valueOf, "HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP");

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

    Set<PotionEffect> defaultEffects = new HashSet<>(INVENTORY_UTILS.getPotionEffects(stack));
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
      } else if (potion.getType() == Material.POTION) { // Sanity check, SpawnablePotionBukkit
        return INVENTORY_UTILS.getPotionEffects(potion);
      }
    }
    return Collections.emptyList();
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

  public static void consumeItem(PlayerEvent event) {
    consumeItem(event, event.getPlayer());
  }

  public static void consumeItem(Event event, Player player) {
    PlayerInventory inv = player.getInventory();
    EquipmentSlot hand = INVENTORY_UTILS.getUsedHand(event);
    ItemStack inHand = inv.getItem(hand);
    if (inHand.getAmount() == 1) {
      inHand = null;
    } else {
      inHand.setAmount(inHand.getAmount() - 1);
    }
    inv.setItem(hand, inHand);
  }

  public interface InventoryUtilsPlatform {
    Collection<PotionEffect> getPotionEffects(ItemStack item);

    boolean isUnbreakable(ItemMeta item);

    default void setUnbreakable(ItemStack item, boolean unbreakable) {
      setUnbreakable(item.getItemMeta(), unbreakable);
    }

    void setUnbreakable(ItemMeta meta, boolean unbreakable);

    boolean openVillager(Villager villager, Player viewer);

    ItemStack craftItemCopy(ItemStack item);

    EquipmentSlot getUsedHand(Event event);

    void setCanDestroy(ItemMeta itemMeta, Set<Material> materials);

    Set<Material> getCanDestroy(ItemMeta itemMeta);

    void setCanPlaceOn(ItemMeta itemMeta, Set<Material> materials);

    Set<Material> getCanPlaceOn(ItemMeta itemMeta);
  }
}

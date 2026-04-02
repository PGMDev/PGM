package tc.oc.pgm.util.inventory;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
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
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.ComponentApplicator;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

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
    addEffects(stack, meta, newEffects);
    stack.setItemMeta(meta);
  }

  public static void addEffects(ItemStack stack, PotionMeta meta, List<PotionEffect> newEffects) {
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
    if (potion.getItemMeta() instanceof PotionMeta meta) {
      if (meta.hasCustomEffects()) {
        return meta.getCustomEffects();
      } else if (Materials.POTIONS.matches(potion)) { // Sanity check, SpawnablePotionBukkit
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

    if (leftover != stack.getAmount()) {
      placed = stack.clone();
      placed.setAmount(amount);
      inv.setItem(slot, placed);

      stack = stack.clone();
      stack.setAmount(leftover);
    }
    return stack;
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

    boolean isViewable(Inventory inventory);

    default Collection<Class<? extends Event>> getRelevantEvents(SlotGroup group) {
      // Legacy needs no additional events
      return List.of();
    }

    default ComponentApplicator parseComponents(Material type, Node components)
        throws InvalidXMLException {
      // Default no-op for legacy
      return ComponentApplicator.EMPTY;
    }

    /**
     * Builder for component applicators
     *
     * @param merge if components will merge with current ones, or override existing ones
     * @return A builder for component applicator
     */
    default ComponentApplicator.Builder applicatorBuilder(boolean merge) {
      return new ComponentApplicatorBuilderImpl(merge);
    }

    class ComponentApplicatorBuilderImpl implements ComponentApplicator.Builder {
      protected final boolean merge;
      private final List<BiConsumer<ItemStack, ItemMeta>> modifiers = new ArrayList<>();
      private ComponentApplicator other = ComponentApplicator.EMPTY;

      public ComponentApplicatorBuilderImpl(boolean merge) {
        this.merge = merge;
      }

      protected void register(Consumer<ItemMeta> metaModifier) {
        modifiers.add((is, meta) -> metaModifier.accept(meta));
      }

      protected <T extends ItemMeta> void register(Class<T> type, Consumer<T> metaModifier) {
        modifiers.add((is, meta) -> {
          if (type.isInstance(meta)) metaModifier.accept(type.cast(meta));
        });
      }

      protected <T extends ItemMeta> void register(
          Class<T> type, BiConsumer<ItemStack, T> metaModifier) {
        modifiers.add((is, meta) -> {
          if (type.isInstance(meta)) metaModifier.accept(is, type.cast(meta));
        });
      }

      @Override
      public void addEnchantments(Map<Enchantment, Integer> enchants) {
        if (enchants.isEmpty()) return;
        register(meta -> InventoryUtils.addEnchantments(meta, enchants));
      }

      @Override
      public void addStoredEnchantments(Map<Enchantment, Integer> enchants) {
        if (enchants.isEmpty()) return;
        register(
            EnchantmentStorageMeta.class,
            meta -> enchants.forEach((e, l) -> meta.addStoredEnchant(e, l, true)));
      }

      @Override
      public void addPotions(List<PotionEffect> potions) {
        if (potions.isEmpty()) return;
        if (merge) {
          register(PotionMeta.class, (is, meta) -> InventoryUtils.addEffects(is, meta, potions));
        } else {
          register(PotionMeta.class, meta -> {
            meta.clearCustomEffects();
            potions.forEach(e -> meta.addCustomEffect(e, false));
          });
        }
      }

      @Override
      public void addAttributeModifiers(
          SetMultimap<Attribute, AttributeModifier> attributeModifiers) {
        if (attributeModifiers.isEmpty()) return;
        register(meta -> ATTRIBUTE_UTILS.applyAttributeModifiers(attributeModifiers, meta));
      }

      @Override
      public void addDisplayName(String name) {
        register(meta -> meta.setDisplayName(name));
      }

      @Override
      public void addColor(Color color) {
        register(LeatherArmorMeta.class, meta -> meta.setColor(color));
      }

      @Override
      public void addLore(List<String> lore) {
        register(meta -> meta.setLore(lore));
      }

      @Override
      public void addItemFlags(ItemFlag... flags) {
        if (flags.length > 0) register(meta -> meta.addItemFlags(flags));
      }

      @Override
      public void addUnbreakable() {
        register(meta -> INVENTORY_UTILS.setUnbreakable(meta, true));
      }

      @Override
      public void addCanDestroy(MaterialMatcher materials) {
        Set<Material> canDestroy = materials.getMaterials();
        if (merge) {
          register(meta -> INVENTORY_UTILS.setCanDestroy(
              meta, unionMaterials(INVENTORY_UTILS.getCanDestroy(meta), canDestroy)));
        } else {
          register(meta -> INVENTORY_UTILS.setCanDestroy(meta, canDestroy));
        }
      }

      @Override
      public void addCanPlaceOn(MaterialMatcher materials) {
        Set<Material> canPlaceOn = materials.getMaterials();
        if (merge) {
          register(meta -> INVENTORY_UTILS.setCanPlaceOn(
              meta, unionMaterials(INVENTORY_UTILS.getCanPlaceOn(meta), canPlaceOn)));
        } else {
          register(meta -> INVENTORY_UTILS.setCanPlaceOn(meta, canPlaceOn));
        }
      }

      @Override
      public void addComponents(ComponentApplicator applicator) {
        this.other = this.other.andThen(applicator);
      }

      @Override
      public ComponentApplicator build() {
        record ComponentApplicatorImpl(List<BiConsumer<ItemStack, ItemMeta>> modifiers)
            implements ComponentApplicator {
          @Override
          public void apply(ItemStack stack) {
            var meta = stack.getItemMeta();
            if (meta != null) { // This happens if the item is "air"
              modifiers.forEach(mod -> mod.accept(stack, meta));
              stack.setItemMeta(meta);
            }
          }
        }

        return new ComponentApplicatorImpl(List.copyOf(modifiers)).andThen(this.other);
      }

      private Set<Material> unionMaterials(Set<Material> a, Set<Material> b) {
        if (a.containsAll(b)) return a;
        if (b.containsAll(a)) return b;

        Set<Material> union = EnumSet.copyOf(a);
        union.addAll(b);
        return union;
      }
    }
  }
}

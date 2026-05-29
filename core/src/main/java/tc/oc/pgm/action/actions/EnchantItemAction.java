package tc.oc.pgm.action.actions;

import java.util.Objects;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.inventory.SlotGroup;
import tc.oc.pgm.util.math.Formula;

public class EnchantItemAction extends AbstractAction<MatchPlayer> {

  private final ItemMatcher matcher;
  private final SlotGroup slots;
  private final Enchantment enchant;
  private final Formula<MatchPlayer> level;

  public EnchantItemAction(
      ItemMatcher matcher, SlotGroup slots, Enchantment enchant, Formula<MatchPlayer> level) {
    super(MatchPlayer.class);
    this.matcher = matcher;
    this.slots = slots;
    this.enchant = enchant;
    this.level = level;
  }

  @Override
  public void trigger(MatchPlayer player) {
    PlayerInventory inv = Objects.requireNonNull(player.getInventory());

    int level = Math.max(0, (int) this.level.applyAsDouble(player));

    slots.forEach(inv, (slot, stack) -> {
      if (matcher.matches(stack)) slot.setItem(inv, enchant(stack, level));
    });
  }

  private ItemStack enchant(ItemStack current, int level) {
    if (current.getEnchantmentLevel(enchant) != level) {
      if (level == 0) current.removeEnchantment(enchant);
      else current.addUnsafeEnchantment(enchant, level);
    }
    return current;
  }
}

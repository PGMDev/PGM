package tc.oc.pgm.action.actions;

import java.util.Objects;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.math.Formula;

public class EnchantItemAction extends AbstractAction<MatchPlayer> {

  private final ItemMatcher matcher;
  private final Enchantment enchant;
  private final Formula<MatchPlayer> level;

  public EnchantItemAction(ItemMatcher matcher, Enchantment enchant, Formula<MatchPlayer> level) {
    super(MatchPlayer.class);
    this.matcher = matcher;
    this.enchant = enchant;
    this.level = level;
  }

  @Override
  public void trigger(MatchPlayer player) {
    PlayerInventory inv = Objects.requireNonNull(player.getInventory());

    int level = Math.max(0, (int) this.level.applyAsDouble(player));

    for (ItemStack current : inv.getArmorContents()) {
      if (current != null && matcher.matches(current)) enchant(current, level);
    }
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack current = inv.getItem(i);
      if (current != null && matcher.matches(current)) enchant(current, level);
    }
  }

  private void enchant(ItemStack current, int level) {
    if (current.getEnchantmentLevel(enchant) != level) {
      if (level == 0) current.removeEnchantment(enchant);
      else current.addUnsafeEnchantment(enchant, level);
    }
  }
}

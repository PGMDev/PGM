package tc.oc.pgm.action.actions;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.inventory.Slot;

public class ReplaceItemAction extends AbstractAction<MatchPlayer> {

  private final ItemMatcher matcher;
  private final ItemStack item;
  private final boolean keepAmount;
  private final boolean keepEnchants;

  public ReplaceItemAction(
      ItemMatcher matcher, ItemStack item, boolean keepAmount, boolean keepEnchants) {
    super(MatchPlayer.class);
    this.matcher = matcher;
    this.item = item;
    this.keepAmount = keepAmount;
    this.keepEnchants = keepEnchants;
  }

  @Override
  public void trigger(MatchPlayer player) {
    PlayerInventory inv = player.getInventory();
    Slot.Player.forEach(inv, (slot, stack) -> {
      if (matcher.matches(stack)) slot.setItem(inv, replaceItem(stack, player));
    });
  }

  private ItemStack replaceItem(ItemStack current, MatchPlayer player) {
    ItemStack newItem = item.clone();
    if (keepAmount) newItem.setAmount(current.getAmount());
    if (keepEnchants) newItem.addEnchantments(current.getEnchantments());
    ItemModifier.apply(newItem, player);
    return newItem;
  }
}

package tc.oc.pgm.action.actions;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.TeamColorApplicator;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.inventory.SlotGroup;

public class ReplaceItemAction extends AbstractAction<MatchPlayer> {

  private final ItemMatcher matcher;
  private final SlotGroup slots;
  private final ItemStack item;
  private final boolean keepAmount;
  private final boolean keepEnchants;

  public ReplaceItemAction(
      ItemMatcher matcher,
      SlotGroup slots,
      ItemStack item,
      boolean keepAmount,
      boolean keepEnchants) {
    super(MatchPlayer.class);
    this.matcher = matcher;
    this.slots = slots;
    this.item = item;
    this.keepAmount = keepAmount;
    this.keepEnchants = keepEnchants;
  }

  @Override
  public void trigger(MatchPlayer player) {
    PlayerInventory inv = player.getInventory();
    slots.forEach(inv, (slot, stack) -> {
      if (matcher.matches(stack)) slot.setItem(inv, replaceItem(stack, player));
    });
  }

  private ItemStack replaceItem(ItemStack current, MatchPlayer player) {
    ItemStack newItem = item.clone();
    if (keepAmount) newItem.setAmount(current.getAmount());
    if (keepEnchants) newItem.addEnchantments(current.getEnchantments());
    TeamColorApplicator.apply(newItem, player);
    return newItem;
  }
}

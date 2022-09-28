package tc.oc.pgm.action.actions;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.ItemMatcher;

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
  public void trigger(MatchPlayer matchPlayer) {
    PlayerInventory inv = matchPlayer.getInventory();

    ItemStack[] armor = inv.getArmorContents();
    for (int i = 0; i < armor.length; i++) {
      ItemStack current = armor[i];
      if (current == null || !matcher.matches(current)) continue;
      armor[i] = replaceItem(current);
    }
    inv.setArmorContents(armor);

    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack current = inv.getItem(i);
      if (current == null || !matcher.matches(current)) continue;

      ItemStack newItem = replaceItem(current);
      inv.setItem(i, newItem);
    }
  }

  private ItemStack replaceItem(ItemStack current) {
    ItemStack newItem = item.clone();
    if (keepAmount) newItem.setAmount(current.getAmount());
    if (keepEnchants) newItem.addEnchantments(current.getEnchantments());
    return newItem;
  }
}

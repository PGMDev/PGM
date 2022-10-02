package tc.oc.pgm.action.actions;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemModifier;
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
  public void trigger(MatchPlayer player) {
    PlayerInventory inv = player.getInventory();

    ItemStack[] armor = inv.getArmorContents();
    for (int i = 0; i < armor.length; i++) {
      ItemStack current = armor[i];
      if (current == null || !matcher.matches(current)) continue;
      armor[i] = replaceItem(current, player);
    }
    inv.setArmorContents(armor);

    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack current = inv.getItem(i);
      if (current == null || !matcher.matches(current)) continue;
      inv.setItem(i, replaceItem(current, player));
    }
  }

  private ItemStack replaceItem(ItemStack current, MatchPlayer player) {
    ItemStack newItem = item.clone();
    if (keepAmount) newItem.setAmount(current.getAmount());
    if (keepEnchants) newItem.addEnchantments(current.getEnchantments());
    ItemModifier.apply(newItem, player);
    return newItem;
  }
}

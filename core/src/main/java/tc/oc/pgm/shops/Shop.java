package tc.oc.pgm.shops;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.kyori.adventure.sound.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;
import tc.oc.pgm.shops.menu.Payment;

@FeatureInfo(name = "shops")
public class Shop implements FeatureDefinition {

  private static Sound PURCHASE_SOUND = sound(key("fire.ignite"), Sound.Source.MASTER, 1f, 1.4f);

  private final String id;
  private final String name;
  private final ImmutableList<Category> categories;

  public Shop(String id, String name, List<Category> categories) {
    this.id = id;
    this.name = name != null ? name : id;
    this.categories = ImmutableList.copyOf(categories);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public ImmutableList<Category> getCategories() {
    return categories;
  }

  public boolean canPurchase(Icon icon, MatchPlayer buyer) {
    if (!buyer.getMatch().isRunning() || !buyer.isParticipating()) return false;
    if (icon.isFree()) return true;

    return icon.getPayments().stream().allMatch(p -> p.hasPayment(buyer.getInventory()));
  }

  public boolean purchase(Icon icon, MatchPlayer buyer) {
    if (!canPurchase(icon, buyer)) {
      if (!buyer.getMatch().isRunning()) {
        buyer.sendWarning(translatable("match.error.noMatch"));
      } else {
        buyer.sendWarning(translatable("shop.currency.insufficient"));
      }
      return false;
    }

    if (!icon.isFree()) {
      PlayerInventory inventory = buyer.getInventory();
      for (Payment payment : icon.getPayments()) {
        int remaining = payment.getPrice();
        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
          ItemStack item = inventory.getItem(slot);
          if (item == null || item.getType() != payment.getCurrency()) continue;
          if (item.getAmount() > remaining) {
            item.setAmount(item.getAmount() - remaining);
            inventory.setItem(slot, item);
            remaining = 0;
          } else {
            inventory.setItem(slot, null);
            remaining -= item.getAmount();
          }
        }
      }
    }

    buyer.getBukkit().updateInventory();
    buyer.applyKit(icon.getKit(), true);
    buyer.playSound(PURCHASE_SOUND);
    return true;
  }
}
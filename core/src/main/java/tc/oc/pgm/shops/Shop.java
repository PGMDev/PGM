package tc.oc.pgm.shops;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;
import tc.oc.pgm.util.bukkit.Sounds;

public class Shop extends SelfIdentifyingFeatureDefinition {

  private final String name;
  private final ImmutableList<Category> categories;

  public Shop(String id, String name, List<Category> categories) {
    super(id);
    this.name = name != null ? name : id;
    this.categories = ImmutableList.copyOf(categories);
  }

  public String getName() {
    return name;
  }

  public List<Category> getCategories() {
    return categories;
  }

  public List<Category> getVisibleCategories(MatchPlayer player) {
    return categories.stream()
        .filter(c -> c.getFilter().query(player).isAllowed())
        .toList();
  }

  public void purchase(Icon icon, MatchPlayer buyer) {
    if (icon.takePayment(buyer)) {
      icon.getAction().trigger(buyer);
      buyer.getBukkit().updateInventory();
      buyer.playSound(Sounds.SHOP_PURCHASE);
    } else if (!buyer.getMatch().isRunning()) {
      buyer.sendWarning(translatable("match.error.noMatch"));
    } else {
      buyer.sendWarning(translatable("shop.currency.insufficient"));
    }
  }

  public void purchaseStack(Icon icon, MatchPlayer buyer) {
    if (!buyer.getMatch().isRunning()) {
      buyer.sendWarning(translatable("match.error.noMatch"));
      return;
    }

    // If not KitNode, do not charge more than once.
    if (!(icon.getAction() instanceof KitNode)) {
      purchase(icon, buyer);
      return;
    }

    int amountPerPurchase = Math.max(1, icon.getItem().getAmount());
    int maxStackSize = icon.getItem().getMaxStackSize();
    int desiredPurchases = maxStackSize / amountPerPurchase;

    // How much can really pay
    int affordablePurchases = desiredPurchases;
    for (tc.oc.pgm.shops.menu.Payment payment : icon.getPayments()) {
      affordablePurchases =
          Math.min(affordablePurchases, payment.getAffordableAmount(buyer.getInventory()));
    }

    if (affordablePurchases <= 0) {
      buyer.sendWarning(translatable("shop.currency.insufficient"));
      return;
    }

    int successfulCharges = 0;
    for (int i = 0; i < affordablePurchases; i++) {
      if (icon.takePayment(buyer)) {
        successfulCharges++;
      } else {
        break;
      }
    }

    if (successfulCharges > 0) {
      triggerBulk(icon.getAction(), buyer, successfulCharges);
      buyer.getBukkit().updateInventory();
      buyer.playSound(Sounds.SHOP_PURCHASE); // Only one sound
    }
  }

  private void triggerBulk(Action<? super MatchPlayer> action, MatchPlayer player, int count) {
    if (count <= 0) return;
    for (int i = 0; i < count; i++) {
      action.trigger(player);
    }
  }
}

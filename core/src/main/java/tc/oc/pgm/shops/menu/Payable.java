package tc.oc.pgm.shops.menu;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;

public interface Payable {

  default boolean isFree() {
    return getPayments().isEmpty() || getPayments().stream().noneMatch(p -> p.getPrice() > 0);
  }

  List<Payment> getPayments();

  default boolean canPurchase(MatchPlayer buyer) {
    return canPurchase(buyer, 1) > 0;
  }

  default int canPurchase(MatchPlayer buyer, int max) {
    if (!buyer.getMatch().isRunning() || !buyer.isParticipating()) return 0;
    if (isFree()) return max;

    int affordable = max;
    for (Payment payment : getPayments()) {
      affordable = payment.getAffordableAmount(buyer.getInventory(), affordable);
      if (affordable <= 0) break;
    }
    return affordable;
  }

  default boolean takePayment(MatchPlayer buyer) {
    return takePayment(buyer, 1) > 0;
  }

  default int takePayment(MatchPlayer buyer, int max) {
    int affordable = canPurchase(buyer, max);
    if (affordable <= 0) return 0;

    if (!isFree()) {
      PlayerInventory inventory = buyer.getInventory();
      for (Payment payment : getPayments()) {
        int remaining = payment.getPrice() * affordable;
        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
          ItemStack item = inventory.getItem(slot);
          if (item == null || !payment.matches(item)) continue;
          if (item.getAmount() > remaining) {
            item.setAmount(item.getAmount() - remaining);
            inventory.setItem(slot, item);
            remaining = 0;
          } else {
            inventory.setItem(slot, null);
            remaining -= item.getAmount();
          }
        }

        // Should never happen, canPurchase checks for payment being available in the inventory
        if (remaining > 0) {
          throw new IllegalStateException("Player couldn't pay for their purchase.");
        }
      }
    }
    return affordable;
  }

  static Payable of(List<Payment> payments) {
    return () -> payments;
  }
}

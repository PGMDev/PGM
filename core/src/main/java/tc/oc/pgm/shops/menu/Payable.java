package tc.oc.pgm.shops.menu;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.Slot;

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
    var inv = buyer.getInventory();
    for (Payment payment : getPayments()) {
      affordable = Math.min(affordable, payment.getAffordableAmount(inv, affordable));
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

        for (var slot : Slot.Storage.storage().toList()) {
          ItemStack item = slot.getItem(inventory);
          if (item == null || !payment.matches(item)) continue;
          if (item.getAmount() > remaining) {
            item.setAmount(item.getAmount() - remaining);
            slot.setItem(inventory, item);
            remaining = 0;
          } else {
            slot.setItem(inventory, null);
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

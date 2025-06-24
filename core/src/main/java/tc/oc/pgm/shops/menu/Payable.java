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
    if (!buyer.getMatch().isRunning() || !buyer.isParticipating()) return false;
    return isFree() || getPayments().stream().allMatch(p -> p.hasPayment(buyer.getInventory()));
  }

  default boolean takePayment(MatchPlayer buyer) {
    if (!canPurchase(buyer)) return false;

    if (!isFree()) {
      PlayerInventory inventory = buyer.getInventory();
      for (Payment payment : getPayments()) {
        int remaining = payment.getPrice();
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
    return true;
  }

  static Payable of(List<Payment> payments) {
    return () -> payments;
  }
}

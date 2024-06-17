package tc.oc.pgm.util.nms.packets;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PlayerPackets {
  void playDeathAnimation(Player player);

  void showBorderWarning(Player player, boolean show);

  void fakePlayerItemPickup(Player player, Item item);

  void sendLegacyHelmet(Player player, ItemStack item);

  void updateVelocity(Player player);
}

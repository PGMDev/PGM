package tc.oc.pgm.util.nms.packets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PlayerPackets {
  void playDeathAnimation(Player player);

  void showBorderWarning(Player player, boolean show);

  void fakePlayerItemPickup(Player player, Entity entity);

  void sendLegacyHelmet(Player player, ItemStack item);

  void updateVelocity(Player player);
}

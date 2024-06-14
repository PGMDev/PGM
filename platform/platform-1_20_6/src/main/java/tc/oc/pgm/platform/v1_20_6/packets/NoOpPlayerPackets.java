package tc.oc.pgm.platform.v1_20_6.packets;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.nms.packets.PlayerPackets;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class NoOpPlayerPackets implements PlayerPackets {
  @Override
  public void playDeathAnimation(Player player) {}

  @Override
  public void showBorderWarning(Player player, boolean show) {}

  @Override
  public void fakePlayerItemPickup(Player player, Item item) {}

  @Override
  public void sendLegacyWearing(Player player, int slot, ItemStack item) {}

  @Override
  public void updateVelocity(Player player) {}
}

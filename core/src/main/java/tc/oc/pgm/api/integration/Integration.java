package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;

public interface Integration {

  public static boolean isFriend(Player a, Player b) {
    return false;
  }

  @Nullable
  public static String getNick(Player player) {
    return null;
  }

  public static boolean isVanished(Player player) {
    return PGM.get().getVanishManager().isVanished(player.getUniqueId());
  }
}

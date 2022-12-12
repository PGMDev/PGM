package tc.oc.pgm.api.integration;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(FriendIntegration.Noop);
  static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(NickIntegration.Noop);
  static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(VanishIntegration.Noop);
  static final AtomicReference<PunishmentIntegration> PUNISHMENTS =
      new AtomicReference<PunishmentIntegration>(PunishmentIntegration.Noop);

  public static void setFriendIntegration(FriendIntegration integration) {
    FRIENDS.set(assertNotNull(integration));
  }

  public static void setNickIntegration(NickIntegration integration) {
    NICKS.set(assertNotNull(integration));
  }

  public static void setVanishIntegration(VanishIntegration integration) {
    VANISH.set(assertNotNull(integration));
  }

  public static void setPunishmentIntegration(PunishmentIntegration integration) {
    PUNISHMENTS.set(assertNotNull(integration));
  }

  public static boolean isFriend(Player a, Player b) {
    return FRIENDS.get().isFriend(a, b);
  }

  @Nullable
  public static String getNick(Player player) {
    return NICKS.get().getNick(player);
  }

  public static boolean isVanished(Player player) {
    return VANISH.get().isVanished(player.getUniqueId());
  }

  public static boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return VANISH.get().setVanished(player, vanish, quiet);
  }

  public static boolean isMuted(Player player) {
    return PUNISHMENTS.get().isMuted(player);
  }

  @Nullable
  public static String getMuteReason(Player player) {
    return PUNISHMENTS.get().getMuteReason(player);
  }
}

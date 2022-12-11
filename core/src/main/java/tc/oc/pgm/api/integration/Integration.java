package tc.oc.pgm.api.integration;

import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.integrations.NoopFriendIntegration;
import tc.oc.pgm.integrations.NoopNickIntegration;
import tc.oc.pgm.integrations.NoopPunishmentIntegration;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new NoopFriendIntegration());
  static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(new NoopNickIntegration());
  static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(PGM.get().getVanishManager());
  static final AtomicReference<PunishmentIntegration> PUNISHMENTS =
      new AtomicReference<PunishmentIntegration>(new NoopPunishmentIntegration());

  public static void setFriendIntegration(FriendIntegration integration) {
    FRIENDS.set(integration);
  }

  public static void setNickIntegration(NickIntegration integration) {
    NICKS.set(integration);
  }

  public static void setVanishIntegration(VanishIntegration integration) {
    VANISH.set(integration);
  }

  public static void setPunishmentIntegration(PunishmentIntegration integration) {
    PUNISHMENTS.set(integration);
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

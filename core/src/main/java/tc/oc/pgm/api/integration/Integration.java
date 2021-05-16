package tc.oc.pgm.api.integration;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.integration.FriendIntegrationImpl;
import tc.oc.pgm.integration.NickIntegrationImpl;
import tc.oc.pgm.integration.PunishmentIntegrationImpl;
import tc.oc.pgm.integration.VanishIntegrationImpl;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new FriendIntegrationImpl());
  static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(new NickIntegrationImpl());
  static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(new VanishIntegrationImpl());
  static final AtomicReference<PunishmentIntegration> PUNISHMENTS =
      new AtomicReference<PunishmentIntegration>(new PunishmentIntegrationImpl());

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

  // FRIENDS
  public static boolean isFriend(Player a, Player b) {
    return FRIENDS.get().isFriend(a, b);
  }

  // NICKS
  @Nullable
  public static String getNick(Player player) {
    return NICKS.get().getNick(player);
  }

  // VANISH
  public static boolean isVanished(Player player) {
    return VANISH.get().isVanished(player);
  }

  public static boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return VANISH.get().setVanished(player, vanish, quiet);
  }

  public static boolean isMuted(Player player) {
    return PUNISHMENTS.get().isMuted(player);
  }

  public static String getMuteReason(Player player) {
    return PUNISHMENTS.get().getMuteReason(player);
  }

  public static boolean isHidden(Player player) {
    return PUNISHMENTS.get().isHidden(player);
  }
}

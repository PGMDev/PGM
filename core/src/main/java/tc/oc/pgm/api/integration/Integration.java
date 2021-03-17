package tc.oc.pgm.api.integration;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new FriendIntegration.FriendIntegrationImpl());
  static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(new NickIntegration.NickIntegrationImpl());
  static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(new VanishIntegration.VanishIntegrationImpl());

  public static void setFriendIntegration(FriendIntegration integration) {
    FRIENDS.set(integration);
  }

  public static void setNickIntegration(NickIntegration integration) {
    NICKS.set(integration);
  }

  public static void setVanishIntegration(VanishIntegration integration) {
    VANISH.set(integration);
  }

  // FRIENDS
  public static boolean isFriend(Player a, Player b) {
    return FRIENDS.get().isFriend(a, b);
  }

  // NICKS
  public static String getNick(Player player) {
    return NICKS.get().getNick(player);
  }

  public static boolean hasNick(Player player) {
    return NICKS.get().hasNick(player);
  }

  // VANISH
  public static boolean isVanished(Player player) {
    return VANISH.get().isVanished(player);
  }

  public static Collection<Player> getVanished() {
    return VANISH.get().getVanished();
  }

  public static boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return VANISH.get().setVanished(player, vanish, quiet);
  }
}

package tc.oc.pgm.api.integration;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new NoopFriendIntegration());
  static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(new NoopNickIntegration());
  static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(new NoopVanishIntegration());
  static final AtomicReference<PunishmentIntegration> PUNISHMENTS =
      new AtomicReference<PunishmentIntegration>(new NoopPunishmentIntegration());

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

  // No-op Implementations

  public class NoopFriendIntegration implements FriendIntegration {
    @Override
    public boolean isFriend(Player a, Player b) {
      return false;
    }
  }

  public class NoopNickIntegration implements NickIntegration {
    @Override
    public @Nullable String getNick(Player player) {
      return null;
    }
  }

  public class NoopPunishmentIntegration implements PunishmentIntegration {

    @Override
    public boolean isMuted(Player player) {
      return false;
    }

    @Override
    public @Nullable String getMuteReason(Player player) {
      return null;
    }
  }

  public class NoopVanishIntegration implements VanishIntegration {
    @Override
    public boolean isVanished(UUID uuid) {
      return false;
    }

    @Override
    public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
      return false;
    }
  }
}

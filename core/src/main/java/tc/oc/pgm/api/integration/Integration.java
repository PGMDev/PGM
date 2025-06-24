package tc.oc.pgm.api.integration;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.player.MatchPlayer;

public final class Integration {

  private Integration() {}

  private static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new NoopFriendIntegration());
  private static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(new NoopNickIntegration());
  private static final AtomicReference<PunishmentIntegration> PUNISHMENTS =
      new AtomicReference<PunishmentIntegration>(new NoopPunishmentIntegration());
  private static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(new NoopVanishIntegration());
  private static final AtomicReference<SquadIntegration> SQUAD =
      new AtomicReference<>(new NoopSquadIntegration());
  private static Set<Channel<?>> CHANNELS = new HashSet<>();

  public static void setFriendIntegration(FriendIntegration integration) {
    FRIENDS.set(assertNotNull(integration));
  }

  public static void setNickIntegration(NickIntegration integration) {
    NICKS.set(assertNotNull(integration));
  }

  public static void setPunishmentIntegration(PunishmentIntegration integration) {
    PUNISHMENTS.set(assertNotNull(integration));
  }

  public static void setVanishIntegration(VanishIntegration integration) {
    VANISH.set(assertNotNull(integration));
  }

  public static void setSquadIntegration(SquadIntegration integration) {
    SQUAD.set(assertNotNull(integration));
  }

  public static void registerChannel(Channel<?> channel) {
    if (CHANNELS == null)
      throw new IllegalStateException(
          "New channels cannot be registered after ChatManager has been initialised!");
    CHANNELS.add(assertNotNull(channel));
  }

  public static boolean isFriend(Player a, Player b) {
    return FRIENDS.get().isFriend(a, b);
  }

  @Nullable
  public static String getNick(Player player) {
    return NICKS.get().getNick(player);
  }

  public static boolean isMuted(Player player) {
    return PUNISHMENTS.get().isMuted(player);
  }

  public static boolean areInSquad(Player a, Player b) {
    return SQUAD.get().areInSquad(a, b);
  }

  @Nullable
  public static Collection<UUID> getSquad(Player player) {
    return SQUAD.get().getSquad(player);
  }

  @Nullable
  public static String getMuteReason(Player player) {
    return PUNISHMENTS.get().getMuteReason(player);
  }

  public static boolean isVanished(Player player) {
    return VANISH.get().isVanished(player.getUniqueId());
  }

  public static boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return VANISH.get().setVanished(player, vanish, quiet);
  }

  public static boolean isDisguised(Player player) {
    return isVanished(player) || getNick(player) != null;
  }

  public static Set<Channel<?>> pollRegisteredChannels() {
    Set<Channel<?>> channels = CHANNELS;
    CHANNELS = null;
    return Collections.unmodifiableSet(channels);
  }

  // No-op Implementations

  private static class NoopFriendIntegration implements FriendIntegration {
    @Override
    public boolean isFriend(Player a, Player b) {
      return false;
    }
  }

  private static class NoopNickIntegration implements NickIntegration {
    @Override
    public @Nullable String getNick(Player player) {
      return null;
    }
  }

  private static class NoopPunishmentIntegration implements PunishmentIntegration {

    @Override
    public boolean isMuted(Player player) {
      return false;
    }

    @Override
    public @Nullable String getMuteReason(Player player) {
      return null;
    }
  }

  private static class NoopVanishIntegration implements VanishIntegration {
    @Override
    public boolean isVanished(UUID uuid) {
      return false;
    }

    @Override
    public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
      return false;
    }
  }

  private static class NoopSquadIntegration implements SquadIntegration {

    @Override
    public boolean areInSquad(Player a, Player b) {
      return false;
    }

    @Override
    public Collection<UUID> getSquad(Player player) {
      return null;
    }
  }
}

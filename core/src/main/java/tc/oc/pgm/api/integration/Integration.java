package tc.oc.pgm.api.integration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.integration.FriendIntegrationImpl;
import tc.oc.pgm.integration.NickIntegrationImpl;
import tc.oc.pgm.integration.PunishmentIntegrationImpl;
import tc.oc.pgm.integration.RequestIntegrationImpl;
import tc.oc.pgm.integration.TranslationIntegrationImpl;
import tc.oc.pgm.integration.VanishIntegrationImpl;
import tc.oc.pgm.util.translation.Translation;

public interface Integration {

  static final AtomicReference<FriendIntegration> FRIENDS =
      new AtomicReference<FriendIntegration>(new FriendIntegrationImpl());
  static final AtomicReference<NickIntegration> NICKS =
      new AtomicReference<NickIntegration>(new NickIntegrationImpl());
  static final AtomicReference<VanishIntegration> VANISH =
      new AtomicReference<VanishIntegration>(new VanishIntegrationImpl());
  static final AtomicReference<PunishmentIntegration> PUNISHMENTS =
      new AtomicReference<PunishmentIntegration>(new PunishmentIntegrationImpl());
  static final AtomicReference<RequestIntegration> REQUESTS =
      new AtomicReference<RequestIntegration>(new RequestIntegrationImpl());
  static final AtomicReference<TranslationIntegration> TRANSLATIONS =
      new AtomicReference<TranslationIntegration>(new TranslationIntegrationImpl());

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

  public static void setRequestIntegration(RequestIntegration integration) {
    REQUESTS.set(integration);
  }

  public static void setTranslationIntegration(TranslationIntegration integration) {
    TRANSLATIONS.set(integration);
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

  // ETC

  // MATCH INFO
  public static List<Component> getExtraMatchInfoLines(MapInfo map) {
    return REQUESTS.get().getExtraMatchInfoLines(map);
  }

  public static boolean isSponsor(MapInfo map) {
    return REQUESTS.get().isSponsor(map);
  }

  // TRANSLATIONS
  public static CompletableFuture<Translation> translate(String message) {
    return TRANSLATIONS.get().translate(message);
  }
}

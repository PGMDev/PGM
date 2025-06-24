package tc.oc.pgm.listeners;

import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.channels.AdminChannel;
import tc.oc.pgm.channels.ChatManager;

@Deprecated(forRemoval = true)
public class ChatDispatcher {

  private static final ChatDispatcher INSTANCE = new ChatDispatcher();

  public static ChatDispatcher get() {
    return INSTANCE; // FIXME: no one should need to statically access ChatDispatcher, but community
    // does this a lot
  }

  public static final TextComponent ADMIN_CHAT_PREFIX = text()
      .append(text("[", NamedTextColor.WHITE))
      .append(text("A", NamedTextColor.GOLD))
      .append(text("] ", NamedTextColor.WHITE))
      .build();

  public static void broadcastAdminChatMessage(Component message, Match match) {
    ChatManager.broadcastAdminMessage(message);
  }

  public static void broadcastAdminChatMessage(
      Component message, Match match, Optional<Sound> sound) {
    AdminChannel channel = PGM.get().getChatManager().getAdminChannel();
    Collection<MatchPlayer> viewers = channel.getBroadcastViewers(null);
    channel.broadcastMessage(message, null);

    sound.ifPresent(s -> {
      viewers.stream()
          .filter(player -> {
            SettingValue settingValue = player.getSettings().getValue(SettingKey.SOUNDS);
            return settingValue.equals(SettingValue.SOUNDS_ALL)
                || settingValue.equals(SettingValue.SOUNDS_CHAT);
          })
          .forEach(player -> player.playSound(s));
    });
  }
}

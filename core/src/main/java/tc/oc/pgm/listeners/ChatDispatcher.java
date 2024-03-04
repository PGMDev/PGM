package tc.oc.pgm.listeners;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.channels.AdminChannel;
import tc.oc.pgm.channels.ChannelManager;

@Deprecated()
public class ChatDispatcher {

  private static final ChatDispatcher INSTANCE = new ChatDispatcher();

  public static ChatDispatcher get() {
    return INSTANCE; // FIXME: no one should need to statically access ChatDispatcher, but community
    // does this a lot
  }

  public static final TextComponent ADMIN_CHAT_PREFIX =
      text()
          .append(text("[", NamedTextColor.WHITE))
          .append(text("A", NamedTextColor.GOLD))
          .append(text("] ", NamedTextColor.WHITE))
          .build();

  private static final Sound DM_SOUND = sound(key("random.orb"), Sound.Source.MASTER, 1f, 1.2f);

  private static final Predicate<MatchPlayer> AC_FILTER =
      viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT);

  public static void broadcastAdminChatMessage(Component message, Match match) {
    ChannelManager.broadcastAdminMessage(message);
  }

  public static void broadcastAdminChatMessage(
      Component message, Match match, Optional<Sound> sound) {
    AdminChannel channel = PGM.get().getChannelManager().getAdminChannel();
    Collection<MatchPlayer> viewers = channel.getBroadcastViewers(null);
    channel.broadcastMessage(message, null);
    if (sound.isPresent()) viewers.forEach(viewer -> playSound(viewer, sound.get()));
  }

  public static void playSound(MatchPlayer player, Sound sound) {
    SettingValue value = player.getSettings().getValue(SettingKey.SOUNDS);
    if (value.equals(SettingValue.SOUNDS_ALL)
        || value.equals(SettingValue.SOUNDS_CHAT)
        || (sound.equals(DM_SOUND) && value.equals(SettingValue.SOUNDS_DM))) {
      player.playSound(sound);
    }
  }
}

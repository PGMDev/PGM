package tc.oc.pgm.channels;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.named.NameStyle;

public class AdminChannel implements Channel<Void> {

  private static final List<String> ALIASES = List.of("a");

  public static final TextComponent PREFIX = text()
      .append(text("[", NamedTextColor.WHITE))
      .append(text("A", NamedTextColor.GOLD))
      .append(text("] ", NamedTextColor.WHITE))
      .build();

  @Override
  public String getDisplayName() {
    return "admin";
  }

  @Override
  public List<String> getAliases() {
    return ALIASES;
  }

  @Override
  public Character getShortcut() {
    return '$';
  }

  @Override
  public SettingValue getSetting() {
    return SettingValue.CHAT_ADMIN;
  }

  @Override
  public String getLoggerFormat(Void target) {
    return "[A] %s: %s";
  }

  @Override
  public boolean canSendMessage(MatchPlayer sender) {
    return sender.getBukkit().hasPermission(Permissions.ADMINCHAT);
  }

  @Override
  public Void getTarget(MatchPlayer sender, CommandContext<CommandSender> arguments) {
    return null;
  }

  @Override
  public Collection<MatchPlayer> getViewers(Void unused) {
    Set<MatchPlayer> players = new HashSet<>();
    PGM.get().getMatchManager().getMatches().forEachRemaining(match -> {
      for (MatchPlayer player : match.getPlayers())
        if (player.getBukkit().hasPermission(Permissions.ADMINCHAT)) players.add(player);
    });
    return players;
  }

  @Override
  public void messageSent(ChannelMessageEvent<Void> event) {
    for (MatchPlayer viewer : event.getViewers()) {
      if (viewer.equals(event.getSender())) continue;
      SettingValue value = viewer.getSettings().getValue(SettingKey.SOUNDS);
      if (value.equals(SettingValue.SOUNDS_ALL) || value.equals(SettingValue.SOUNDS_CHAT))
        viewer.playSound(Sounds.ADMIN_CHAT);
    }
  }

  @Override
  public Component formatMessage(Void target, @Nullable MatchPlayer sender, Component message) {
    return text()
        .append(PREFIX)
        .append(
            sender != null
                ? text()
                    .append(sender.getName(NameStyle.VERBOSE))
                    .append(text(": ", NamedTextColor.WHITE))
                    .build()
                : empty())
        .append(message)
        .build();
  }
}

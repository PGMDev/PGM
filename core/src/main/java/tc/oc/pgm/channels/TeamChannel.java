package tc.oc.pgm.channels;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.named.NameStyle;

public class TeamChannel implements Channel<Party> {

  private static final List<String> ALIASES = List.of("t");

  @Override
  public String getDisplayName() {
    return "team";
  }

  @Override
  public List<String> getAliases() {
    return ALIASES;
  }

  @Override
  public SettingValue getSetting() {
    return SettingValue.CHAT_TEAM;
  }

  @Override
  public String getLoggerFormat(Party target) {
    return "(" + target.getNameLegacy() + ") %s: %s";
  }

  @Override
  public boolean supportsRedirect() {
    return true;
  }

  @Override
  public Party getTarget(MatchPlayer sender, CommandContext<CommandSender> arguments) {
    return sender.getParty();
  }

  @Override
  public Collection<MatchPlayer> getViewers(Party target) {
    return target.getMatch().getPlayers().stream()
        .filter(viewer -> target.equals(viewer.getParty())
            || (viewer.isObserving() && viewer.getBukkit().hasPermission(Permissions.ADMINCHAT)))
        .collect(Collectors.toList());
  }

  @Override
  public Collection<MatchPlayer> getBroadcastViewers(Party target) {
    return target.getMatch().getPlayers().stream()
        .filter(viewer -> target.equals(viewer.getParty()) || viewer.isObserving())
        .collect(Collectors.toList());
  }

  @Override
  public Component formatMessage(Party target, @Nullable MatchPlayer sender, Component message) {
    return text()
        .append(target.getChatPrefix())
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

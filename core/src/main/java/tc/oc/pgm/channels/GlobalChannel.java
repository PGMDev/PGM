package tc.oc.pgm.channels;

import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.named.NameStyle;

public class GlobalChannel implements Channel<Void> {

  private static final List<String> ALIASES = List.of("g", "all", "shout");

  @Override
  public String getDisplayName() {
    return "global";
  }

  @Override
  public List<String> getAliases() {
    return ALIASES;
  }

  @Override
  public Character getShortcut() {
    return '!';
  }

  @Override
  public SettingValue getSetting() {
    return SettingValue.CHAT_GLOBAL;
  }

  @Override
  public boolean supportsRedirect() {
    return true;
  }

  @Override
  public Void getTarget(MatchPlayer sender, CommandContext<CommandSender> arguments) {
    return null;
  }

  @Override
  public Collection<MatchPlayer> getViewers(Void unused) {
    Set<MatchPlayer> players = new HashSet<>();
    PGM.get()
        .getMatchManager()
        .getMatches()
        .forEachRemaining(match -> players.addAll(match.getPlayers()));
    return players;
  }

  @Override
  public Component formatMessage(Void target, @Nullable MatchPlayer sender, Component message) {
    if (sender == null) return message;
    return text()
        .append(text("<", NamedTextColor.WHITE))
        .append(sender.getName(NameStyle.VERBOSE))
        .append(text(">: ", NamedTextColor.WHITE))
        .append(message)
        .build();
  }
}

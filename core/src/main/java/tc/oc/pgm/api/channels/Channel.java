package tc.oc.pgm.api.channels;

import static net.kyori.adventure.text.Component.text;

import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.named.NameStyle;

public interface Channel<T> {

  default String getDisplayName() {
    return getAliases()[0];
  }

  String[] getAliases();

  @Nullable
  default Character getShortcut() {
    return null;
  }

  default SettingValue getSetting() {
    return null;
  }

  default boolean supportsRedirect() {
    return false;
  }

  default boolean canSendMessage(MatchPlayer sender) {
    return true;
  }

  T getTarget(MatchPlayer sender, Map<String, ?> arguments);

  Collection<MatchPlayer> getViewers(T target);

  default Collection<MatchPlayer> getBroadcastViewers(T target) {
    return getViewers(target);
  }

  default void sendMessage(ChannelMessageEvent<T> event) {}

  default Component formatMessage(T target, @Nullable MatchPlayer sender, Component message) {
    if (sender == null) return message;
    return text()
        .append(text("<", NamedTextColor.WHITE))
        .append(sender.getName(NameStyle.VERBOSE))
        .append(text(">: ", NamedTextColor.WHITE))
        .append(message)
        .build();
  }

  default void registerCommand(PaperCommandManager<CommandSender> manager) {
    String name = getAliases()[0];
    String[] aliases = new String[getAliases().length - 1];
    System.arraycopy(getAliases(), 1, aliases, 0, aliases.length);

    manager.command(
        manager
            .commandBuilder(name, aliases)
            .handler(
                context -> {
                  MatchPlayer sender =
                      context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);
                  PGM.get().getChannelManager().setChannel(sender, this);
                }));

    manager.command(
        manager
            .commandBuilder(name, aliases)
            .argument(
                StringArgument.<CommandSender>builder("message")
                    .greedy()
                    .withSuggestionsProvider(Players::suggestPlayers)
                    .build())
            .handler(
                context -> {
                  MatchPlayer sender =
                      context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);
                  PGM.get().getChannelManager().process(this, sender, context.asMap());
                }));
  }

  default Map<String, Object> processChatShortcut(MatchPlayer sender, String message) {
    Map<String, Object> arguments = new HashMap<String, Object>();
    if (message.length() == 1) {
      PGM.get().getChannelManager().setChannel(sender, this);
      return arguments;
    }

    arguments.put("message", message.substring(1).trim());
    return arguments;
  }

  default void broadcastMessage(Component component, T target) {
    broadcastMessage(component, target, player -> true);
  }

  default void broadcastMessage(Component component, T target, Predicate<MatchPlayer> filter) {
    Collection<MatchPlayer> viewers = getBroadcastViewers(target);
    Component finalMessage = formatMessage(target, null, component);
    viewers.stream().filter(filter).forEach(player -> player.sendMessage(finalMessage));
  }
}

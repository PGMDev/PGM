package tc.oc.pgm.channels;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.usage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.suggestion.SuggestionProvider;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.MessageSenderIdentity;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.bukkit.OnlinePlayerUUIDMapAdapter;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.named.NameStyle;

public class PrivateMessageChannel implements Channel<MatchPlayer> {

  private static final List<String> ALIASES = List.of("msg", "tell", "r");

  private static final CloudKey<MatchPlayer> TARGET_KEY = CloudKey.of("target", MatchPlayer.class);

  private final OnlinePlayerUUIDMapAdapter<MessageSenderIdentity> selectedPlayer, lastMessagedBy;

  public PrivateMessageChannel() {
    this.selectedPlayer = new OnlinePlayerUUIDMapAdapter<>(PGM.get());
    this.lastMessagedBy = new OnlinePlayerUUIDMapAdapter<>(PGM.get());
  }

  @Override
  public String getDisplayName() {
    return "private messages";
  }

  @Override
  public List<String> getAliases() {
    return ALIASES;
  }

  @Override
  public Character getShortcut() {
    return '@';
  }

  @Override
  public String getLoggerFormat(MatchPlayer target) {
    return "(DM) %s -> " + target.getBukkit().getDisplayName() + ": %s";
  }

  @Override
  public MatchPlayer getTarget(MatchPlayer sender, CommandContext<CommandSender> arguments) {
    MatchPlayer target = arguments.get(TARGET_KEY);
    // Check the user can message the target
    checkPermissions(target, sender);
    return target;
  }

  @Override
  public Collection<MatchPlayer> getViewers(MatchPlayer target) {
    if (target == null) throw exception("command.playerNotFound");
    return Collections.singletonList(target);
  }

  @Override
  public void messageSent(ChannelMessageEvent<MatchPlayer> event) {
    MatchPlayer sender = Objects.requireNonNull(event.getSender());
    MatchPlayer target = event.getTarget();

    sender.sendMessage(formatMessage(target, "to", event.getComponent()));

    SettingValue value = target.getSettings().getValue(SettingKey.SOUNDS);
    if (value.equals(SettingValue.SOUNDS_ALL)
        || value.equals(SettingValue.SOUNDS_CHAT)
        || value.equals(SettingValue.SOUNDS_DM)) target.playSound(Sounds.DIRECT_MESSAGE);

    setTarget(lastMessagedBy, sender, target);
    setTarget(lastMessagedBy, target, sender);
  }

  @Override
  public Component formatMessage(MatchPlayer target, MatchPlayer sender, Component message) {
    return formatMessage(sender, "from", message);
  }

  public Component formatMessage(MatchPlayer player, String direction, Component message) {
    return text()
        .append(translatable("misc." + direction, NamedTextColor.GRAY, TextDecoration.ITALIC))
        .append(space())
        .append(player.getName(NameStyle.VERBOSE))
        .append(text(": ", NamedTextColor.WHITE))
        .append(message)
        .build();
  }

  @Override
  public void registerCommand(CommandManager<CommandSender> manager) {
    manager.command(manager
        .commandBuilder("msg", "tell")
        .required(CommandComponent.<CommandSender, MatchPlayer>builder()
            .key(TARGET_KEY)
            .commandManager(manager))
        .optional(
            MESSAGE_KEY,
            greedyStringParser(),
            SuggestionProvider.blockingStrings(Players::suggestPlayers))
        .handler(context -> {
          MatchPlayer sender =
              context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);

          if (!context.contains(MESSAGE_KEY)) {
            setTarget(selectedPlayer, sender, context.get(TARGET_KEY));
            PGM.get().getChatManager().setChannel(sender, this);
          } else {
            PGM.get().getChatManager().process(this, sender, context);
          }
        }));

    manager.command(manager
        .commandBuilder("reply", "r")
        .optional(
            MESSAGE_KEY,
            greedyStringParser(),
            SuggestionProvider.blockingStrings(Players::suggestPlayers))
        .handler(context -> {
          MatchPlayer sender =
              context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);
          MatchPlayer target = getTarget(lastMessagedBy, sender);
          if (target == null) throw exception("command.message.noReply", text("/msg"));

          if (!context.contains(MESSAGE_KEY)) {
            setTarget(selectedPlayer, sender, target);
            PGM.get().getChatManager().setChannel(sender, this);
          } else {
            context.store(TARGET_KEY, target);
            PGM.get().getChatManager().process(this, sender, context);
          }
        }));
  }

  @Override
  public void processChatMessage(
      MatchPlayer sender, String message, CommandContext<CommandSender> context) {
    Channel.super.processChatMessage(sender, message, context);
    MatchPlayer target = getTarget(selectedPlayer, sender);
    if (target != null) context.store(TARGET_KEY, target);
  }

  @Override
  public void processChatShortcut(
      MatchPlayer sender, String message, CommandContext<CommandSender> context) {
    if (message.length() == 1) throw usage(getShortcut() + "<player> [message]");

    int spaceIndex = message.indexOf(' ');
    MatchPlayer target = Players.getMatchPlayer(
        sender.getBukkit(), message.substring(1, spaceIndex == -1 ? message.length() : spaceIndex));
    if (target == null) throw exception("command.playerNotFound");

    if (spaceIndex == -1) {
      setTarget(selectedPlayer, sender, target);
      PGM.get().getChatManager().setChannel(sender, this);
      return;
    }

    context.store(MESSAGE_KEY, message.substring(spaceIndex + 1).trim());
    context.store(TARGET_KEY, target);
  }

  private void checkPermissions(MatchPlayer target, MatchPlayer sender) {
    if (sender.equals(target)) throw exception("command.message.self");

    SettingValue option = sender.getSettings().getValue(SettingKey.MESSAGE);
    if (option.equals(SettingValue.MESSAGE_OFF))
      throw exception(
          "command.message.disabled",
          text("/toggle dm", NamedTextColor.RED).clickEvent(runCommand("/toggle dm")));
    if (option.equals(SettingValue.MESSAGE_FRIEND)
        && !Integration.isFriend(target.getBukkit(), sender.getBukkit()))
      throw exception(
          "command.message.disabled",
          text("/toggle dm", NamedTextColor.RED).clickEvent(runCommand("/toggle dm")));

    option = target.getSettings().getValue(SettingKey.MESSAGE);
    if (!sender.getBukkit().hasPermission(Permissions.STAFF)) {
      if (option.equals(SettingValue.MESSAGE_OFF))
        throw exception("command.message.blocked", target.getName());

      if (option.equals(SettingValue.MESSAGE_FRIEND)
          && !Integration.isFriend(target.getBukkit(), sender.getBukkit()))
        throw exception("command.message.friendsOnly", target.getName());

      if (Integration.isMuted(target.getBukkit()))
        throw exception("moderation.mute.target", target.getName());
    }
  }

  public MatchPlayer getTarget(
      OnlinePlayerUUIDMapAdapter<MessageSenderIdentity> store, MatchPlayer sender) {
    MessageSenderIdentity targetIdentity = store.get(sender.getId());
    if (targetIdentity == null) return null;

    MatchPlayer target = targetIdentity.getPlayer(sender.getBukkit());
    if (target != null) checkPermissions(target, sender);

    return target;
  }

  public void setTarget(
      OnlinePlayerUUIDMapAdapter<MessageSenderIdentity> store,
      MatchPlayer sender,
      MatchPlayer target) {
    checkPermissions(target, sender);
    store.compute(sender.getId(), (uuid, identity) -> {
      if (identity != null && identity.getPlayer(sender.getBukkit()) == target) {
        return identity;
      }

      return new MessageSenderIdentity(sender.getBukkit(), target.getBukkit());
    });
  }

  public boolean canSendVanished(MatchPlayer sender, CommandContext<CommandSender> context) {
    return context
        .optional(TARGET_KEY)
        .map(target -> Players.isVisible(target.getBukkit(), sender.getBukkit()))
        .orElse(false);
  }
}

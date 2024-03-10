package tc.oc.pgm.channels;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.*;

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import io.leangen.geantyref.TypeToken;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
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
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.named.NameStyle;

public class MessageChannel implements Channel<MatchPlayer> {

  private final ChannelManager manager;
  private final OnlinePlayerMapAdapter<MessageSenderIdentity> setChannel, lastMessagedBy;

  private static final Sound SOUND = sound(key("random.orb"), Sound.Source.MASTER, 1f, 1.2f);

  public MessageChannel(ChannelManager manager) {
    this.manager = manager;
    this.setChannel = new OnlinePlayerMapAdapter<MessageSenderIdentity>(PGM.get());
    this.lastMessagedBy = new OnlinePlayerMapAdapter<MessageSenderIdentity>(PGM.get());
  }

  @Override
  public String getDisplayName() {
    return "messages";
  }

  @Override
  public String[] getAliases() {
    return new String[] {"msg", "tell", "r"};
  }

  @Override
  public Character getShortcut() {
    return '@';
  }

  @Override
  public MatchPlayer getTarget(MatchPlayer sender, Map<String, ?> arguments) {
    MatchPlayer target = (MatchPlayer) arguments.get("target");
    if (target == null) {
      return getSelectedTarget(sender);
    } else {
      checkSettings(target, sender);

      if (!target.equals(getSelectedTarget(sender))
          && !(manager.getSelectedChannel(sender) instanceof MessageChannel)) {
        this.lastMessagedBy.put(
            sender.getBukkit(), new MessageSenderIdentity(sender.getBukkit(), target.getBukkit()));
      }

      if (!sender.equals(getSelectedTarget(target))
          && !(manager.getSelectedChannel(target) instanceof MessageChannel)) {
        this.lastMessagedBy.put(
            target.getBukkit(), new MessageSenderIdentity(target.getBukkit(), sender.getBukkit()));
      }

      return target;
    }
  }

  @Override
  public Collection<MatchPlayer> getViewers(MatchPlayer target) {
    if (target == null) throw exception("command.playerNotFound");
    return Collections.singletonList(target);
  }

  @Override
  public void sendMessage(ChannelMessageEvent<MatchPlayer> event) {
    MatchPlayer sender = event.getSender();
    MatchPlayer target = event.getTarget();

    SettingValue value = target.getSettings().getValue(SettingKey.SOUNDS);
    if (value.equals(SettingValue.SOUNDS_ALL)
        || value.equals(SettingValue.SOUNDS_CHAT)
        || value.equals(SettingValue.SOUNDS_DM)) target.playSound(SOUND);

    Bukkit.getPluginManager()
        .callEvent(
            new ChannelMessageEvent<MatchPlayer>(
                event.getChannel(),
                sender,
                event.getTarget(),
                Collections.singletonList(sender),
                event.getMessage()));
  }

  @Override
  public Component formatMessage(
      MatchPlayer target, @Nullable MatchPlayer sender, Component message) {
    if (!target.equals(sender))
      return text()
          .append(translatable("misc.to", NamedTextColor.GRAY, TextDecoration.ITALIC))
          .append(space())
          .append(target.getName(NameStyle.VERBOSE))
          .append(text(": ", NamedTextColor.WHITE))
          .append(message)
          .build();

    return text()
        .append(translatable("misc.from", NamedTextColor.GRAY, TextDecoration.ITALIC))
        .append(space())
        .append(sender.getName(NameStyle.VERBOSE))
        .append(text(": ", NamedTextColor.WHITE))
        .append(message)
        .build();
  }

  @Override
  public void registerCommand(PaperCommandManager<CommandSender> manager) {
    manager.command(
        manager
            .commandBuilder("msg", "tell")
            .argument(
                StringArgument.<CommandSender, MatchPlayer>ofType(
                        TypeToken.get(MatchPlayer.class), "target")
                    .withParser(
                        manager
                            .getParserRegistry()
                            .createParser(
                                TypeToken.get(MatchPlayer.class), ParserParameters.empty())
                            .orElseThrow(IllegalStateException::new))
                    .build())
            .handler(
                context -> {
                  MatchPlayer sender =
                      context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);
                  final MatchPlayer target = context.<MatchPlayer>get("target");
                  setSelectedTarget(sender, target);
                  PGM.get().getChannelManager().setChannel(sender, this);
                }));

    manager.command(
        manager
            .commandBuilder("msg", "tell")
            .argument(
                StringArgument.<CommandSender, MatchPlayer>ofType(
                        TypeToken.get(MatchPlayer.class), "target")
                    .withParser(
                        manager
                            .getParserRegistry()
                            .createParser(
                                TypeToken.get(MatchPlayer.class), ParserParameters.empty())
                            .orElseThrow(IllegalStateException::new))
                    .build())
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

    manager.command(
        manager
            .commandBuilder("reply", "r")
            .handler(
                context -> {
                  MatchPlayer sender =
                      context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);
                  MatchPlayer target = getLastMessagedBy(sender);
                  if (target == null) throw exception("command.message.noReply", text("/msg"));

                  setSelectedTarget(sender, target);
                  PGM.get().getChannelManager().setChannel(sender, this);
                }));

    manager.command(
        manager
            .commandBuilder("reply", "r")
            .argument(
                StringArgument.<CommandSender>builder("message")
                    .greedy()
                    .withSuggestionsProvider(Players::suggestPlayers)
                    .build())
            .handler(
                context -> {
                  MatchPlayer sender =
                      context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);
                  MatchPlayer target = getLastMessagedBy(sender);
                  if (target == null) throw exception("command.message.noReply", text("/msg"));
                  context.set("target", target);

                  PGM.get().getChannelManager().process(this, sender, context.asMap());
                }));
  }

  @Override
  public Map<String, Object> processChatShortcut(MatchPlayer sender, String message) {
    if (message.length() == 1) throw usage("/msg <player> [message]");
    Map<String, Object> arguments = new HashMap<String, Object>();

    int spaceIndex = message.indexOf(" ");
    MatchPlayer target =
        Players.getMatchPlayer(
            sender.getBukkit(),
            message.substring(1, spaceIndex == -1 ? message.length() : spaceIndex));
    if (target == null) throw exception("command.playerNotFound");

    if (spaceIndex == -1) {
      setSelectedTarget(sender, target);
      PGM.get().getChannelManager().setChannel(sender, this);
    } else {
      arguments.put("message", message.substring(spaceIndex + 1));
      arguments.put("target", target);
    }

    return arguments;
  }

  private void checkSettings(MatchPlayer target, MatchPlayer sender) {
    if (sender.equals(target)) throw exception("command.message.self");

    SettingValue option = sender.getSettings().getValue(SettingKey.MESSAGE);
    if (option.equals(SettingValue.MESSAGE_OFF))
      throw exception("command.message.disabled", text("/toggle dm", NamedTextColor.RED));
    if (option.equals(SettingValue.MESSAGE_FRIEND)
        && !Integration.isFriend(target.getBukkit(), sender.getBukkit()))
      throw exception("command.message.disabled", text("/toggle dm", NamedTextColor.RED));

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

  public void setSelectedTarget(MatchPlayer sender, MatchPlayer target) {
    checkSettings(target, sender);
    setChannel.put(
        sender.getBukkit(), new MessageSenderIdentity(sender.getBukkit(), target.getBukkit()));
  }

  public MatchPlayer getSelectedTarget(MatchPlayer sender) {
    MessageSenderIdentity targetIdentity = setChannel.get(sender.getBukkit());
    if (targetIdentity == null) return null;

    MatchPlayer target = targetIdentity.getPlayer(sender.getBukkit());
    if (target != null) checkSettings(target, sender);

    return target;
  }

  public MatchPlayer getLastMessagedBy(MatchPlayer sender) {
    MessageSenderIdentity targetIdentity = lastMessagedBy.get(sender.getBukkit());
    if (targetIdentity == null) return null;

    MatchPlayer target = targetIdentity.getPlayer(sender.getBukkit());
    if (target != null) checkSettings(target, sender);

    return target;
  }
}

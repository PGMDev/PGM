package tc.oc.pgm.util.chat;

import static tc.oc.pgm.util.TimeUtils.fromTicks;

import java.util.Collection;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/** Receiver of chat messages, sounds, titles, and other media. */
@FunctionalInterface
public interface Audience extends ForwardingAudience.Single {

  @Deprecated
  default void sendMessage(String message) {
    sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
  }

  @Deprecated
  default void sendWarning(Component message) {
    sendMessage(
        Component.text(" \u26a0 ", NamedTextColor.YELLOW)
            .append(message.colorIfAbsent(NamedTextColor.RED)));
    playSound(Sound.sound(Key.key("note.bass"), Sound.Source.MASTER, 1f, 0.75f));
  }

  @Deprecated
  default void showTitle(
      Component title, Component subTitle, int inTicks, int stayTicks, int outTicks) {
    showTitle(
        Title.title(
            title,
            subTitle,
            Title.Times.of(fromTicks(inTicks), fromTicks(stayTicks), fromTicks(outTicks))));
  }

  @Deprecated BukkitAudiences PROVIDER = BukkitAudiences.create(BukkitUtils.getPlugin());

  static Audience get(CommandSender sender) {
    return () -> PROVIDER.sender(sender);
  }

  static Audience get(Collection<? extends CommandSender> senders) {
    return () -> PROVIDER.filter(senders::contains);
  }

  static Audience empty() {
    return net.kyori.adventure.audience.Audience::empty;
  }
}

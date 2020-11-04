package tc.oc.pgm.util.chat;

import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/** Receiver of chat messages, sounds, titles, and other media. */
@FunctionalInterface
public interface Audience extends ForwardingAudience.Single {

  Sound WARNING_SOUND = Sound.sound(Key.key("note.bass"), Sound.Source.MASTER, 1f, 0.75f);
  Component WARNING_MESSAGE = text(" \u26a0 ", NamedTextColor.YELLOW); // ⚠

  default void sendWarning(Component message) {
    sendMessage(WARNING_MESSAGE.append(message.colorIfAbsent(NamedTextColor.RED)));
    playSound(WARNING_SOUND);
  }

  BukkitAudiences PROVIDER = BukkitAudiences.create(BukkitUtils.getPlugin());

  static Audience console() {
    return PROVIDER::console;
  }

  static Audience get(CommandSender sender) {
    return () -> PROVIDER.sender(sender);
  }

  static Audience get(Collection<? extends CommandSender> senders) {
    return () -> PROVIDER.filter(senders::contains);
  }

  /** Makes a single audience from a group of audiences */
  static Audience get(Iterable<? extends net.kyori.adventure.audience.Audience> audiences) {
    return () -> net.kyori.adventure.audience.Audience.audience(audiences);
  }

  static Audience empty() {
    return net.kyori.adventure.audience.Audience::empty;
  }
}

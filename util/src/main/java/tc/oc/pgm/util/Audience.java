package tc.oc.pgm.util;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.ComponentRenderer;

/** Receiver of chat messages, sounds, titles, and other media. */
@FunctionalInterface
public interface Audience extends ForwardingAudience.Single {

  Sound WARNING_SOUND = sound(key("note.bass"), Sound.Source.MASTER, 1f, 0.75f);
  Component WARNING_MESSAGE = text(" \u26a0 ", NamedTextColor.YELLOW); // ⚠

  default void sendWarning(ComponentLike message) {
    sendMessage(WARNING_MESSAGE.append(message.asComponent().colorIfAbsent(NamedTextColor.RED)));
    playSound(WARNING_SOUND);
  }

  BukkitAudiences PROVIDER =
      BukkitAudiences.builder(BukkitUtils.getPlugin())
          .componentRenderer(ComponentRenderer.RENDERER)
          .build();

  static Audience console() {
    return PROVIDER::console;
  }

  static Audience get(@NotNull CommandSender sender) {
    return () -> PROVIDER.sender(sender);
  }

  static Audience get(Collection<? extends CommandSender> senders) {
    return () -> PROVIDER.filter(senders::contains);
  }

  /** Makes a single audience from a group of audiences */
  static Audience get(Iterable<? extends net.kyori.adventure.audience.Audience> audiences) {
    return () -> net.kyori.adventure.audience.Audience.audience(audiences);
  }

  /** Filter out an audience from a group of audiences */
  static <T extends Audience> Audience filter(Predicate<T> filter, Collection<T> audiences) {
    return get(audiences.stream().filter(filter).collect(Collectors.toList()));
  }

  static Audience empty() {
    return net.kyori.adventure.audience.Audience::empty;
  }

  static @NotNull Collector<? super net.kyori.adventure.audience.Audience, ?, Audience>
      toAudience() {
    return Collectors.collectingAndThen(
        Collectors.toCollection(ArrayList::new),
        audiences -> get(Collections.unmodifiableCollection(audiences)));
  }
}

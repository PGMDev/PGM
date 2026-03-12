package tc.oc.pgm.util;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.text.ComponentRenderer;

/** Receiver of chat messages, sounds, titles, and other media. */
@FunctionalInterface
public interface Audience extends ForwardingAudience.Single {

  Component WARNING_MESSAGE = text(" \u26a0 ", NamedTextColor.YELLOW); // ⚠

  default void sendWarning(ComponentLike message) {
    sendMessage(WARNING_MESSAGE.append(message.asComponent().colorIfAbsent(NamedTextColor.RED)));
    playSound(Sounds.WARNING);
  }

  default void playSound(Sound sound, Location location) {
    this.playSound(sound, location.getX(), location.getY(), location.getZ());
  }

  BukkitAudiences PROVIDER = BukkitAudiences.builder(BukkitUtils.getPlugin())
      .componentRenderer(ComponentRenderer.RENDERER)
      .build();

  float soundDistance = 4096f;
  float maxVolume = 0.9999f;

  /**
   * Plays a sound "globally", without a particular location.
   *
   * <p>For non-global sounds, use {@link Audience#playSound(Sound, Location)}.
   *
   * @param sound a sound
   */
  @Override
  default void playSound(@NotNull Sound sound) {
    this.playSound(sound, true);
  }

  default void playSound(@NotNull Sound sound, boolean global) {
    var player = pointers().get(Identity.UUID).map(Bukkit::getPlayer);
    omnipresent:
    if (global) {
      if (player.isEmpty()) break omnipresent;
      // account for MC-146721 only on affected clients for affected sounds
      if (Sounds.MODERN_GLOBAL_SOUNDS.contains(sound.name().value())) break omnipresent;
      var location = player.get().getEyeLocation();
      var realVolume =
          soundDistance / (16f * (1f - Math.max(0f, Math.min(maxVolume, sound.volume()))));
      this.playSound(
          Sound.sound(sound).volume(realVolume).build(),
          location.getX(),
          location.getY() + soundDistance,
          location.getZ());
      return;
    }

    Single.super.playSound(sound);
  }

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

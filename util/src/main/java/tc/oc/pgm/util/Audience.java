package tc.oc.pgm.util;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponentProvider;

/** Receiver of chat messages, sounds, titles, and other media. */
@FunctionalInterface
public interface Audience extends ForwardingAudience.Single {

  Sound WARNING_SOUND = sound(key("note.bass"), Sound.Source.MASTER, 1f, 0.75f);
  Component WARNING_MESSAGE = text(" \u26a0 ", NamedTextColor.YELLOW); // ⚠

  default void sendWarning(ComponentLike message) {
    sendMessage(WARNING_MESSAGE.append(message.asComponent().colorIfAbsent(NamedTextColor.RED)));
    playSound(WARNING_SOUND);
  }

  static final String PATTERN = "\\<[@!].*?:[0-" + NameStyle.values().length + "]\\>";

  ComponentRenderer<Pointered> RENDERER =
      new ComponentRenderer<Pointered>() {
        @Override
        public Component render(Component component, final Pointered context) {
          component =
              component.replaceText(
                  TextReplacementConfig.builder()
                      .match(PATTERN)
                      .replacement(
                          (match, b) -> {
                            String input = match.group();
                            String[] parts = input.split(":");
                            if (parts.length == 2) {
                              String id = parts[0].substring(2, parts[0].length());
                              String style = parts[1].substring(0, parts[1].length() - 1);
                              NameStyle ns = NameStyle.values()[Integer.parseInt(style)];
                              return PlayerComponentProvider.render(id, ns, context);
                            }
                            return text("");
                          })
                      .build());

          return GlobalTranslator.render(
              component, context.get(Identity.LOCALE).orElse(Locale.ROOT));
        }
      };

  BukkitAudiences PROVIDER =
      BukkitAudiences.builder(BukkitUtils.getPlugin()).componentRenderer(RENDERER).build();

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

  /** Filter out an audience from a group of audiences */
  static <T extends Audience> Audience filter(Predicate<T> filter, Collection<T> audiences) {
    return get(audiences.stream().filter(filter).collect(Collectors.toList()));
  }

  static Audience empty() {
    return net.kyori.adventure.audience.Audience::empty;
  }
}

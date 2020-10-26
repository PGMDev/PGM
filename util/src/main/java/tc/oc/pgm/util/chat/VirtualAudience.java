package tc.oc.pgm.util.chat;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextTranslations;

/** An {@link Audience} that represents a virtual {@link CommandSender}. */
@FunctionalInterface
public interface VirtualAudience extends Audience {

  // FIXME
  BukkitAudiences PLATFORM = BukkitAudiences.create(BukkitUtils.getPlugin());

  /**
   * Get the {@link CommandSender} of this audience.
   *
   * @return A command sender.
   */
  CommandSender getAudience();

  /**
   * Render a {@link net.kyori.adventure.text.Component} to be relevant to this audience.
   *
   * @param message A message.
   * @return A rendered message.
   */
  default Component renderMessage(Component message) {
    return TextTranslations.translate(message, getAudience().getLocale());
  }

  @Override
  default void sendMessage(Component message) {
    PLATFORM.sender(getAudience()).sendMessage(renderMessage(message));
  }

  @Override
  default void sendWarning(Component message) {
    sendMessage(
        Component.text(" \u26a0 ", NamedTextColor.YELLOW)
            .append(message.colorIfAbsent(NamedTextColor.RED)));
    playSound(new Sound("note.bass", 1f, 0.75f));
  }

  @Override
  default void showHotbar(Component hotbar) {}

  @Override
  default void showBossbar(Component message, float progress) {}

  @Override
  default void showTitle(
      Component title, Component subTitle, int inTicks, int stayTicks, int outTicks) {}

  @Override
  default void sendMessage(String message) {
    getAudience().sendMessage(message);
  }

  @Override
  default void playSound(Sound sound) {}
}

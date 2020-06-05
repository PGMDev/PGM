package tc.oc.pgm.util.chat;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.text.TextTranslations;

/** An {@link Audience} that represents a virtual {@link CommandSender}. */
@FunctionalInterface
public interface VirtualAudience extends Audience {

  /**
   * Get the {@link CommandSender} of this audience.
   *
   * @return A command sender.
   */
  CommandSender getAudience();

  /**
   * Render a {@link net.kyori.text.Component} to be relevant to this audience.
   *
   * @param message A message.
   * @return A rendered message.
   */
  default Component renderMessage(Component message) {
    return TextTranslations.translate(message, getAudience().getLocale());
  }

  @Override
  default void sendMessage(Component message) {
    TextAdapter.sendComponent(getAudience(), renderMessage(message));
  }

  @Override
  default void sendWarning(Component message) {
    sendMessage(
        TextComponent.of(" \u26a0 ", TextColor.YELLOW)
            .append(message.colorIfAbsent(TextColor.RED)));
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

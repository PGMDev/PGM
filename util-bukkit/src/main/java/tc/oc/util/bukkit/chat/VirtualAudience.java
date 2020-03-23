package tc.oc.util.bukkit.chat;

import java.util.Locale;
import javax.annotation.Nullable;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentRenderers;
import tc.oc.util.bukkit.translations2.ComponentProvider;

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
  default net.kyori.text.Component renderMessage(net.kyori.text.Component message) {
    return TRANSLATION_PROVIDER.getComponent(message, getAudience().getLocale());
  }

  /**
   * Render a {@link net.kyori.text.Component} to be relevant to this audience.
   *
   * @param message A message.
   * @return A rendered message, as "legacy" Minecraft text.
   */
  default String renderMessageLegacy(net.kyori.text.Component message) {
    return TRANSLATION_PROVIDER.getLegacy(message, getAudience().getLocale());
  }

  /**
   * A global, static translator of messages with the default locale as English.
   *
   * @see #renderMessage(net.kyori.text.Component)
   * @see #renderMessageLegacy(net.kyori.text.Component)
   */
  ComponentProvider TRANSLATION_PROVIDER = new ComponentProvider(null, Locale.US);

  @Override
  default void sendMessage(net.kyori.text.Component message) {
    TextAdapter.sendComponent(getAudience(), renderMessage(message));
  }

  @Override
  default void sendWarning(net.kyori.text.Component message) {
    sendMessage(
        TextComponent.of(" \u26a0 ", TextColor.YELLOW).append(message).color(TextColor.RED));
    playSound(new Sound("note.bass", 1f, 0.75f));
  }

  @Override
  default void showHotbar(net.kyori.text.Component hotbar) {}

  @Override
  default void showBossbar(net.kyori.text.Component message, float progress) {}

  ///////////////////////////////////////////////////////////////
  // METHODS BELOW ARE ALL DEPRECATED AND WILL BE REMOVED SOON //
  ///////////////////////////////////////////////////////////////

  @Override
  default void sendMessage(String message) {
    getAudience().sendMessage(message);
  }

  @Override
  default void sendMessage(Component message) {
    ComponentRenderers.send(getAudience(), message);
  }

  @Override
  default void sendHotbarMessage(Component message) {}

  @Override
  default void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {}

  @Override
  default void playSound(Sound sound) {}
}

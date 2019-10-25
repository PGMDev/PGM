package tc.oc.chat;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;

/**
 * Receiver of chat messages, sounds, titles, and other informational media. Can represent any
 * number of actual recipients.
 */
public interface Audience {

  /** Send a message to chat */
  void sendMessage(Component message);

  /** Send a message to chat styled as a warning, with an optional audio cue */
  default void sendWarning(Component message, boolean audible) {
    sendMessage(
        new PersonalizedText(ChatColor.RED)
            .extra(new PersonalizedText(" \u26a0 ", ChatColor.YELLOW), message));
  }

  /** Play a sound (by raw asset name) */
  void playSound(Sound sound);

  /** Send a message to the display slot above the hotbar */
  void sendHotbarMessage(Component message);

  /** Show a title and/or subtitle */
  void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks);

  /** Use {@link #sendMessage(Component) */
  @Deprecated
  void sendMessage(String message);

  /** Use {@link #sendWarning(Component, boolean) */
  @Deprecated
  default void sendWarning(String message, boolean audible) {
    sendMessage(ChatColor.YELLOW + " \u26a0 " + ChatColor.RED + message);
  }
}

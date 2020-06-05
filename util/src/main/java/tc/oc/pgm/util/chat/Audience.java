package tc.oc.pgm.util.chat;

import net.kyori.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Receiver of chat messages, sounds, titles, and other media. */
public interface Audience {

  /**
   * Send a chat message.
   *
   * @param message A message.
   */
  void sendMessage(Component message);

  /**
   * Send a warning chat message, with an audio cue.
   *
   * @param message A message.
   */
  void sendWarning(Component message);

  /**
   * Send a message above their hotbar.
   *
   * @param message A message.
   */
  void showHotbar(Component message);

  /**
   * Send a message as their boss bar, with a progress meter.
   *
   * @param message A message.
   * @param progress The progress of the bar, represented as [0, 1].
   */
  void showBossbar(Component message, float progress);

  /**
   * Send a title with desired lengths.
   *
   * @param title The upper line
   * @param subTitle The lower line
   * @param inTicks Fade in time in ticks
   * @param stayTicks Length title is displayed
   * @param outTicks Fade out time in ticks
   * @return
   */
  void showTitle(Component title, Component subTitle, int inTicks, int stayTicks, int outTicks);

  /**
   * Play a {@link Sound}, by the raw asset name.
   *
   * @param sound The sound.
   */
  void playSound(Sound sound);

  ///////////////////////////////////////////////////////////////
  // METHODS BELOW ARE ALL DEPRECATED AND WILL BE REMOVED SOON //
  ///////////////////////////////////////////////////////////////

  @Deprecated
  void sendMessage(String message);

  @Deprecated
  default void sendWarning(String message, boolean audible) {
    sendMessage(ChatColor.YELLOW + " \u26a0 " + ChatColor.RED + message);
  }

  @Deprecated
  static Audience get(CommandSender sender) {
    if (sender instanceof Player) {
      return (PlayerAudience) () -> (Player) sender;
    } else {
      return (VirtualAudience) () -> sender;
    }
  }
}

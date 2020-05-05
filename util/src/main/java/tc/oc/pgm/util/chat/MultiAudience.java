package tc.oc.pgm.util.chat;

import javax.annotation.Nullable;
import tc.oc.pgm.util.component.Component;

/** An {@link Audience} with multiple members. */
@FunctionalInterface
public interface MultiAudience extends Audience {

  /**
   * Get all the {@link Audience} members.
   *
   * @return A collection of audiences.
   */
  Iterable<? extends Audience> getAudiences();

  @Override
  default void sendMessage(net.kyori.text.Component message) {
    for (Audience a : getAudiences()) a.sendMessage(message);
  }

  @Override
  default void sendWarning(net.kyori.text.Component message) {
    for (Audience a : getAudiences()) a.sendWarning(message);
  }

  @Override
  default void showHotbar(net.kyori.text.Component message) {
    for (Audience a : getAudiences()) a.showHotbar(message);
  }

  @Override
  default void showBossbar(net.kyori.text.Component message, float progress) {
    for (Audience a : getAudiences()) a.showBossbar(message, progress);
  }

  @Override
  default void playSound(Sound sound) {
    for (Audience a : getAudiences()) a.playSound(sound);
  }

  @Override
  default void showTitle(
      @Nullable net.kyori.text.Component title,
      @Nullable net.kyori.text.Component subTitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    for (Audience a : getAudiences()) a.showTitle(title, subTitle, inTicks, stayTicks, outTicks);
  }

  ///////////////////////////////////////////////////////////////
  // METHODS BELOW ARE ALL DEPRECATED AND WILL BE REMOVED SOON //
  ///////////////////////////////////////////////////////////////

  @Deprecated
  @Override
  default void sendMessage(Component message) {
    for (Audience a : getAudiences()) a.sendMessage(message);
  }

  @Deprecated
  @Override
  default void sendWarning(Component message, boolean audible) {
    for (Audience a : getAudiences()) a.sendWarning(message, audible);
  }

  @Deprecated
  @Override
  default void sendHotbarMessage(Component message) {
    for (Audience a : getAudiences()) a.sendHotbarMessage(message);
  }

  @Deprecated
  @Override
  default void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    for (Audience a : getAudiences()) a.showTitle(title, subtitle, inTicks, stayTicks, outTicks);
  }

  @Deprecated
  @Override
  default void sendMessage(String message) {
    for (Audience a : getAudiences()) a.sendMessage(message);
  }

  @Deprecated
  @Override
  default void sendWarning(String message, boolean audible) {
    for (Audience a : getAudiences()) a.sendWarning(message, audible);
  }
}

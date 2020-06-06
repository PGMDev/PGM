package tc.oc.pgm.util.chat;

import net.kyori.text.Component;

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
  default void sendMessage(Component message) {
    for (Audience a : getAudiences()) a.sendMessage(message);
  }

  @Override
  default void sendWarning(Component message) {
    for (Audience a : getAudiences()) a.sendWarning(message);
  }

  @Override
  default void showHotbar(Component message) {
    for (Audience a : getAudiences()) a.showHotbar(message);
  }

  @Override
  default void showBossbar(Component message, float progress) {
    for (Audience a : getAudiences()) a.showBossbar(message, progress);
  }

  @Override
  default void playSound(Sound sound) {
    for (Audience a : getAudiences()) a.playSound(sound);
  }

  @Override
  default void showTitle(
      Component title, Component subTitle, int inTicks, int stayTicks, int outTicks) {
    for (Audience a : getAudiences()) a.showTitle(title, subTitle, inTicks, stayTicks, outTicks);
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

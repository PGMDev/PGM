package tc.oc.chat;

import javax.annotation.Nullable;
import tc.oc.component.Component;

public interface MultiAudience extends Audience {

  Iterable<? extends Audience> getAudiences();

  @Override
  default void sendMessage(Component message) {
    for (Audience a : getAudiences()) a.sendMessage(message);
  }

  @Override
  default void sendWarning(Component message, boolean audible) {
    for (Audience a : getAudiences()) a.sendWarning(message, audible);
  }

  @Override
  default void playSound(Sound sound) {
    for (Audience a : getAudiences()) a.playSound(sound);
  }

  @Override
  default void sendHotbarMessage(Component message) {
    for (Audience a : getAudiences()) a.sendHotbarMessage(message);
  }

  @Override
  default void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    for (Audience a : getAudiences()) a.showTitle(title, subtitle, inTicks, stayTicks, outTicks);
  }

  @Override
  default void sendMessage(String message) {
    for (Audience a : getAudiences()) a.sendMessage(message);
  }

  @Override
  default void sendWarning(String message, boolean audible) {
    for (Audience a : getAudiences()) a.sendWarning(message, audible);
  }
}

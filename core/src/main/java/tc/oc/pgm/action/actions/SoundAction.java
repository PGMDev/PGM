package tc.oc.pgm.action.actions;

import net.kyori.adventure.sound.Sound;
import tc.oc.pgm.util.Audience;

public class SoundAction extends AbstractAction<Audience> {
  private final Sound sound;

  public SoundAction(Sound sound) {
    super(Audience.class);
    this.sound = sound;
  }

  @Override
  public void trigger(Audience audience) {
    audience.playSound(sound);
  }
}

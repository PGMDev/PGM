package tc.oc.pgm.util.chat;

import javax.annotation.Nullable;
import org.bukkit.util.Vector;

/** A {@link Sound} that is played to an {@link Audience}. */
public final class Sound {
  public final String name;
  public final float volume;
  public final float pitch;
  public final @Nullable Vector location;

  public Sound(String name, float volume, float pitch, @Nullable Vector location) {
    this.name = name;
    this.volume = volume;
    this.pitch = pitch;
    this.location = location;
  }

  public Sound(String name, float volume, float pitch) {
    this(name, volume, pitch, null);
  }

  public Sound(String name, @Nullable Vector location) {
    this(name, 1, 1, location);
  }

  public Sound(String name) {
    this(name, null);
  }
}

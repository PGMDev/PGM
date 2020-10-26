package tc.oc.pgm.broadcast;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.filter.Filter;

public class Broadcast implements Comparable<Broadcast> {
  public enum Type {
    TIP(
        Component.translatable("misc.tip", NamedTextColor.BLUE),
        Sound.sound(Key.key("mob.endermen.idle"), Sound.Source.MASTER, 1, 1.2f)),

    ALERT(
        Component.translatable("misc.alert", NamedTextColor.YELLOW),
        Sound.sound(Key.key("note.pling"), Sound.Source.MASTER, 1, 2f));

    final Component prefix;
    final Sound sound;

    Type(Component prefix, Sound sound) {
      this.prefix = prefix;
      this.sound = sound;
    }

    public Component format(Component message) {
      return Component.text()
          .append(Component.text("["))
          .append(prefix)
          .append(Component.text("] "))
          .append(
              message
                  .color(NamedTextColor.AQUA)
                  .decoration(TextDecoration.BOLD, false)
                  .decoration(TextDecoration.ITALIC, true))
          .colorIfAbsent(NamedTextColor.GRAY)
          .decoration(TextDecoration.BOLD, true)
          .build();
    }
  }

  public final Type type;
  public final Duration after;
  public final int count;
  public final @Nullable Duration every;
  public final Component message;
  public final @Nullable Filter filter;

  public Broadcast(
      Type type,
      Duration after,
      int count,
      @Nullable Duration every,
      Component message,
      @Nullable Filter filter) {
    this.type = checkNotNull(type);
    this.after = checkNotNull(after);
    this.count = count;
    this.every = every;
    this.message = checkNotNull(message);
    this.filter = filter;
  }

  @Override
  public int compareTo(@Nonnull Broadcast o) {
    return this.after.compareTo(o.after);
  }

  public Component getFormattedMessage() {
    return type.format(message);
  }

  public Sound getSound() {
    return this.type.sound;
  }
}

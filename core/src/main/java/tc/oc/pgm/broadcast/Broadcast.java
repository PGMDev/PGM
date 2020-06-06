package tc.oc.pgm.broadcast;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.util.chat.Sound;

public class Broadcast implements Comparable<Broadcast> {
  public enum Type {
    TIP(
        TranslatableComponent.of("misc.tip", TextColor.BLUE),
        new Sound("mob.endermen.idle", 1, 1.2f)),

    ALERT(TranslatableComponent.of("misc.alert", TextColor.YELLOW), new Sound("note.pling", 1, 2f));

    final Component prefix;
    final Sound sound;

    Type(Component prefix, Sound sound) {
      this.prefix = prefix;
      this.sound = sound;
    }

    public Component format(Component message) {
      return TextComponent.builder()
          .append("[")
          .append(prefix)
          .append("] ")
          .append(
              message
                  .color(TextColor.AQUA)
                  .decoration(TextDecoration.BOLD, false)
                  .decoration(TextDecoration.ITALIC, true))
          .colorIfAbsent(TextColor.GRAY)
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

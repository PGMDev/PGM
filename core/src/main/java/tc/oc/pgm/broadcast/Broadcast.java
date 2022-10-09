package tc.oc.pgm.broadcast;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;

import java.time.Duration;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;

public class Broadcast implements Comparable<Broadcast> {
  public enum Type {
    TIP(
        translatable("misc.tip", NamedTextColor.BLUE),
        sound(key("mob.endermen.idle"), Sound.Source.MASTER, 1, 1.2f)),

    ALERT(
        translatable("misc.alert", NamedTextColor.YELLOW),
        sound(key("note.pling"), Sound.Source.MASTER, 1, 2f));

    final Component prefix;
    final Sound sound;

    Type(Component prefix, Sound sound) {
      this.prefix = prefix;
      this.sound = sound;
    }

    public Component format(Component message) {
      return text()
          .append(text("["))
          .append(prefix)
          .append(text("] "))
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
    this.type = assertNotNull(type);
    this.after = assertNotNull(after);
    this.count = count;
    this.every = every;
    this.message = assertNotNull(message);
    this.filter = filter;
  }

  @Override
  public int compareTo(@NotNull Broadcast o) {
    return this.after.compareTo(o.after);
  }

  public Component getFormattedMessage() {
    return type.format(message);
  }

  public Sound getSound() {
    return this.type.sound;
  }
}

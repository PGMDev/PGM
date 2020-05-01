package tc.oc.pgm.broadcast;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

public class Broadcast implements Comparable<Broadcast> {
  public enum Type {
    TIP(
        new PersonalizedText(new PersonalizedTranslatable("misc.tip"), ChatColor.BLUE),
        new Sound("mob.endermen.idle", 1, 1.2f)),

    ALERT(
        new PersonalizedText(new PersonalizedTranslatable("misc.alert"), ChatColor.YELLOW),
        new Sound("note.pling", 1, 2f));

    final Component prefix;
    final Sound sound;

    Type(Component prefix, Sound sound) {
      this.prefix = prefix;
      this.sound = sound;
    }

    public Component format(Component message) {
      return new PersonalizedText(ChatColor.GRAY, ChatColor.BOLD)
          .extra("[")
          .extra(prefix)
          .extra("] ")
          .extra(new PersonalizedText(message, ChatColor.AQUA, ChatColor.ITALIC).bold(false));
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

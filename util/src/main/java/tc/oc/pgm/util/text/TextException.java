package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextTranslations.translate;

import com.google.common.collect.Range;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** An exception with a localized error message. */
public class TextException extends RuntimeException
    implements ComponentMessageThrowable, ComponentLike {

  private final Component message;

  private TextException(
      @Nullable Throwable cause, @Nullable String suggestion, String key, Component... args) {
    super(key, cause);
    final boolean suggest = suggestion != null;
    this.message =
        translatable()
            .key(key)
            .args(args)
            .color(NamedTextColor.RED)
            .append(suggest ? space() : empty())
            .append(suggest ? translatable("error.suggestionSuffix", text(suggestion)) : empty())
            .build();
  }

  @Override
  public @NotNull Component componentMessage() {
    return this.message;
  }

  @Override
  public @NotNull Component asComponent() {
    return this.message;
  }

  @Override
  public String getLocalizedMessage() {
    final Component localized = translate(message);
    return PlainTextComponentSerializer.plainText().serialize(localized);
  }

  public static TextException exception(String key, Component... args) {
    return new TextException(null, null, key, args);
  }

  public static TextException noPermission() {
    return exception("misc.noPermission");
  }

  public static TextException playerOnly() {
    return exception("command.onlyPlayers");
  }

  public static TextException unknown(@Nullable Throwable cause) {
    return new TextException(cause, null, "error.unknown");
  }

  public static TextException usage(String usage) {
    return exception("command.incorrectUsage", text(usage));
  }

  public static TextException invalidFormat(String text, Class<?> type) {
    return invalidFormat(text, type, null);
  }

  public static TextException invalidFormat(String text, Class<?> type, @Nullable Throwable cause) {
    return invalidFormat(text, type, null, cause);
  }

  public static TextException invalidFormat(
      String text, Class<?> type, @Nullable String suggestion, @Nullable Throwable cause) {
    return new TextException(cause, suggestion, "error.invalidFormat", text(text), format(type));
  }

  public static TextException outOfRange(String text, Range<?> range) {
    return new TextException(null, null, "error.outOfRange", text(text), format(range));
  }

  public static TextException outOfRange(Number num, Range<?> range) {
    return outOfRange(num.toString(), range);
  }

  private static Component format(Range<?> range) {
    return text(range.toString().replace("∞", "oo").replace("‥", ", "));
  }

  private static Component format(Class<?> type) {
    return translatable("type." + type.getSimpleName().toLowerCase(Locale.ROOT));
  }
}

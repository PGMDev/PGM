package tc.oc.pgm.util.text;

import com.google.common.collect.Range;
import java.util.Locale;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

/** An exception with a localized error message. */
public class TextException extends RuntimeException {

  private final Component message;

  private TextException(
      @Nullable Throwable cause, @Nullable String suggestion, String key, Component... args) {
    super(key, cause);
    final boolean suggest = suggestion != null;
    this.message =
        Component.translatable()
            .key(key)
            .args(args)
            .color(NamedTextColor.RED)
            .append(suggest ? Component.space() : Component.empty())
            .append(
                suggest
                    ? Component.translatable("error.suggestionSuffix", Component.text(suggestion))
                    : Component.empty())
            .build();
  }

  public Component getText() {
    return message;
  }

  @Override
  public String getLocalizedMessage() {
    final Component localized = TextTranslations.translate(message, Locale.getDefault());
    return PlainComponentSerializer.plain().serialize(localized);
  }

  public static TextException of(String key, Component... args) {
    return new TextException(null, null, key, args);
  }

  public static TextException noPermission() {
    return TextException.of("misc.noPermission");
  }

  public static TextException unknown(@Nullable Throwable cause) {
    return new TextException(cause, null, "error.unknown");
  }

  public static TextException invalidFormat(String text, Class<?> type, @Nullable Throwable cause) {
    return invalidFormat(text, type, null, cause);
  }

  public static TextException invalidFormat(
      String text, Class<?> type, @Nullable String suggestion, @Nullable Throwable cause) {
    return new TextException(
        cause, suggestion, "error.invalidFormat", Component.text(text), format(type));
  }

  public static TextException outOfRange(String text, Range<?> range) {
    return new TextException(null, null, "error.outOfRange", Component.text(text), format(range));
  }

  private static Component format(Range<?> range) {
    return Component.text(range.toString().replace("∞", "oo").replace("‥", ", "));
  }

  private static Component format(Class<?> type) {
    return Component.translatable("type." + type.getSimpleName().toLowerCase());
  }
}

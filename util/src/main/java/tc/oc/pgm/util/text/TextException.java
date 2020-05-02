package tc.oc.pgm.util.text;

import com.google.common.collect.Range;
import java.util.Locale;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

/** An exception with a localized error message. */
public class TextException extends RuntimeException {

  private final Component message;

  private TextException(
      @Nullable Throwable cause, @Nullable String suggestion, String key, Component... args) {
    super(key, cause);
    final boolean suggest = suggestion != null;
    this.message =
        TranslatableComponent.builder(key)
            .args(args)
            .color(TextColor.RED)
            .append(suggest ? TextComponent.space() : TextComponent.empty())
            .append(
                suggest
                    ? TranslatableComponent.of(
                        "error.suggestionSuffix", TextComponent.of(suggestion))
                    : TextComponent.empty())
            .build();
  }

  public Component getText() {
    return message;
  }

  @Override
  public String getLocalizedMessage() {
    final Component localized = TextTranslations.translate(message, Locale.getDefault());
    return PlainComponentSerializer.INSTANCE.serialize(localized);
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
        cause, suggestion, "error.invalidFormat", TextComponent.of(text), format(type));
  }

  public static TextException outOfRange(String text, Range<?> range) {
    return new TextException(null, null, "error.outOfRange", TextComponent.of(text), format(range));
  }

  private static Component format(Range<?> range) {
    return TextComponent.of(range.toString().replace("∞", "oo").replace("‥", ", "));
  }

  private static Component format(Class<?> type) {
    return TranslatableComponent.of("type." + type.getSimpleName().toLowerCase());
  }
}

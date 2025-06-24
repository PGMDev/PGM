package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.translatable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tc.oc.pgm.util.text.TextTranslations.translate;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

public final class TextExceptionTest {
  @Test
  void testGetLocalizedMessage() {
    final Throwable cause = new IllegalStateException();
    final TextException error = TextException.unknown(cause);

    assertEquals(cause, error.getCause(), "error cause is wrong");
    assertEquals("error.unknown", error.getMessage(), "error key is wrong");

    final Component unknownErrorTranslatable = translatable().key("error.unknown").build();
    // accessing tc.oc.pgm.util.Audience crashes the test due to Bukkit not being available
    final Component translatedError = translate(unknownErrorTranslatable, Audience.empty());
    final String serializedError =
        PlainTextComponentSerializer.plainText().serialize(translatedError);

    assertEquals(serializedError, error.getLocalizedMessage(), "localized message is wrong");
  }
}

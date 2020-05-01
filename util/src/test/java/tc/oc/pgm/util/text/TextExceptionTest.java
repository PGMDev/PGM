package tc.oc.pgm.util.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

public final class TextExceptionTest {

  @Test
  void testGetLocalizedMessage() {
    final Throwable cause = new IllegalStateException();
    final TextException error = TextException.unknown(cause);

    assertEquals(cause, error.getCause(), "error cause is wrong");
    assertEquals("error.unknown", error.getMessage(), "error key is wrong");

    // This test can only be run when the JVM has its language set to English or Root
    final Locale jvmLocale = Locale.getDefault();
    assumeTrue(jvmLocale == Locale.ROOT || jvmLocale == Locale.US, "jvm locale is not english");
    assertEquals(
        "An unknown error occurred, please see console for details.", error.getLocalizedMessage());
  }
}

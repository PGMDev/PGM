package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tc.oc.pgm.util.text.NumberComponent.number;

import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public final class NumberComponentTest {

  static {
    Locale.setDefault(Locale.ROOT);
  }

  @ParameterizedTest
  @CsvSource({
    "-10,    -10",
    "-1000,  -1000",
    "-5000,  -5000",
    "0,      0",
    "10,     10",
    "1000,   1000",
    "5000,   5000",
  })
  void testIntegers(double value, String expected) {
    assertEquals(text(expected), number(value));
  }

  @ParameterizedTest
  @CsvSource({
    "-0.1,    -0.1",
    "-0.15,   -0.15",
    "-100.5,  -100.5",
    "-100.53, -100.53",
    "-100.537, -100.54",
    "0.1,     0.1",
    "0.15,    0.15",
    "100.5,   100.5",
    "100.53,  100.53",
    "100.537, 100.54",
  })
  void testDecimals(double value, String expected) {
    assertEquals(text(expected), number(value));
  }

  @ParameterizedTest
  @CsvSource({
    "-10000, -10k",
    "-10001, -10k",
    "-10009, -10.01k",
    "-10010, -10.01k",
    "-10100, -10.1k",
    "-15530, -15.53k",
    "-15537, -15.54k",
    "10000,  10k",
    "10001,  10k",
    "10009,  10.01k",
    "10010,  10.01k",
    "10100,  10.1k",
    "15530,  15.53k",
    "15537,  15.54k",
  })
  void testShort(double value, String expected) {
    assertEquals(text(expected), number(value));
  }
}

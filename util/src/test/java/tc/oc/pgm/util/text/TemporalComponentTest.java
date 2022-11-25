package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tc.oc.pgm.util.text.TemporalComponent.clock;
import static tc.oc.pgm.util.text.TemporalComponent.duration;
import static tc.oc.pgm.util.text.TemporalComponent.ticker;

import java.time.Duration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public final class TemporalComponentTest {
  @ParameterizedTest
  @CsvSource({
    "misc.now,     1,  -1",
    "misc.now,     1,  0",
    "misc.second,  1,  1",
    "misc.seconds, 59, 59",
    "misc.minute,  1,  60",
    "misc.minute,  1,  61",
    "misc.minutes, 2,  120",
    "misc.hour,    1,  3601",
    "misc.hours,   3,  12000",
    "misc.day,     1,  86401",
    "misc.days,    6,  520000",
    "misc.week,    1,  610000",
    "misc.weeks,   3,  2000000",
    "misc.month,   1,  2600000",
    "misc.months,  11, 30000000",
    "misc.year,    1,  32000000",
    "misc.years,   2,  64000000",
    "misc.eon,     1,  999999999",
  })
  void testDurationSeconds(String key, long units, long seconds) {
    final TextColor color = NamedTextColor.GOLD;
    assertEquals(translatable(key, text(units, color)), duration(seconds, color));
  }

  @ParameterizedTest
  @CsvSource({
    "misc.now,     1,  0",
    "misc.second,  1,  -999",
    "misc.seconds, 2,  -1001",
    "misc.minute,  1,  60001"
  })
  void testDuration(String key, long units, long milliseconds) {
    assertEquals(translatable(key, text(units)), duration(Duration.ofMillis(milliseconds)));
  }

  @ParameterizedTest
  @CsvSource({
    "<1s, 0",
    "1s,  1",
    "59s, 59",
    "1m,  119",
    "9m,  540",
  })
  void testTicker(String expected, long seconds) {
    assertEquals(text(expected), ticker(seconds));
  }

  @ParameterizedTest
  @CsvSource({
    "00:00,    0",
    "00:01,    1",
    "00:59,    59",
    "01:00,    60",
    "59:59,    3599",
    "1:00:00,  3600",
    "2:22:22,  8542",
    "10:00:00, 36000",
  })
  void testClock(String expected, long seconds) {
    assertEquals(text(expected), clock(seconds));
  }
}

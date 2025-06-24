package tc.oc.pgm.util.text;

import static org.junit.jupiter.api.Assertions.*;
import static tc.oc.pgm.util.text.TextParser.*;

import com.google.common.collect.Range;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.Version;

@SuppressWarnings("deprecation")
public final class TextParserTest {

  @ParameterizedTest
  @ValueSource(strings = {"true", "on", "yes"})
  void testParseBooleanTrue(String text) throws TextException {
    assertTrue(parseBoolean(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "off", "no"})
  void testParseBooleanFalse(String text) throws TextException {
    assertFalse(parseBoolean(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "notaboolean", "1"})
  void testParseBooleanInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseBoolean(text)).getMessage());
  }

  @ParameterizedTest
  @CsvSource({"1, 1", "-10, -10", "1337, +1337"})
  void testParseInteger(int expected, String text) throws TextException {
    assertEquals(expected, parseInteger(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "notaninteger", "1.0", "-13.01"})
  void testParseIntegerInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseInteger(text)).getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-oo", "0", "-1337"})
  void testParseIntegerOutOfRange(String text) {
    assertEquals(
        "error.outOfRange",
        assertThrows(TextException.class, () -> parseInteger(text, Range.greaterThan(0)))
            .getMessage());
  }

  @ParameterizedTest
  @CsvSource({"1.0,1.0", "-10,-10", "1337.1234,+1337.1234"})
  void testParseFloat(float expected, String text) throws TextException {
    assertEquals(expected, parseFloat(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "notafloat", "1/2"})
  void testParseFloatInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseFloat(text)).getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-oo", "0.0", "-1337.1234"})
  void testParseFloatOutOfRange(String text) {
    assertEquals(
        "error.outOfRange",
        assertThrows(TextException.class, () -> parseFloat(text, Range.greaterThan(0f)))
            .getMessage());
  }

  @ParameterizedTest
  @CsvSource({
    "1000,1s",
    "-120000,-2m",
    "259200000,3d",
    "61000,1m1s",
    "86461000,1d1m1s",
    "0,0",
    "-1234,-1.234"
  })
  void testParseDuration(long millis, String text) throws TextException {
    assertEquals(Duration.ofMillis(millis), parseDuration(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "notaduration", "1/2", "1m2d"})
  void testParseDurationInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseDuration(text)).getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1m", "-oo", "0", "-60"})
  void testParseDurationOutOfRange(String text) {
    assertEquals(
        "error.outOfRange",
        assertThrows(
                TextException.class, () -> parseDuration(text, Range.greaterThan(Duration.ZERO)))
            .getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"oo", "+oo"})
  void testParsePositiveInfinity(String text) throws TextException {
    assertEquals(Integer.MAX_VALUE, parseInteger(text));
    assertEquals(Float.POSITIVE_INFINITY, parseFloat(text));
    assertEquals(TimeUtils.INFINITE_DURATION, parseDuration(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-oo"})
  void testParseNegativeInfinity(String text) throws TextException {
    assertEquals(Integer.MIN_VALUE, parseInteger(text));
    assertEquals(Float.NEGATIVE_INFINITY, parseFloat(text));
    assertEquals(TimeUtils.INFINITE_DURATION.negated(), parseDuration(text));
  }

  @ParameterizedTest
  @CsvSource(
      value = {"0;0;0;0,0,0", "-10.050;2000.123;10;-10.050,2000.123,10"},
      delimiter = ';')
  void testParseVector(float x, float y, float z, String text) throws TextException {
    assertEquals(new Vector(x, y, z), parseVector3d(text));

    // Same test for a 2d vector, but exclude the last pair
    text = text.substring(0, text.lastIndexOf(','));
    assertEquals(new Vector(x, 0, y), parseVector2d(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "notavector", "1.2.3", "1;2;3", "1.01,2.31"})
  void testParseVectorInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseVector3d(text)).getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.25,1337.13", "oo,oo", "0,+oo", "0.001,-0.001"})
  void testParseVectorOutOfRange(String text) {
    assertEquals(
        "error.outOfRange",
        assertThrows(TextException.class, () -> parseVector2d(text, Range.atMost(0f)))
            .getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0,0,0,0", "0,1,0,0.1", "1,2,3,1.2.3"})
  void testParseVersion(int major, int minor, int patch, String text) throws TextException {
    assertEquals(new Version(major, minor, patch), parseVersion(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "1.2.3.4", "v1", "v1.2.3"})
  void testParseVersionInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseVersion(text)).getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.4.1", "2.0", "-1.0.1"})
  void testParseVersionOutOfRange(String text) {
    assertEquals(
        "error.outOfRange",
        assertThrows(
                TextException.class, () -> parseVersion(text, Range.atMost(new Version(1, 4, 0))))
            .getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"DARK_PURPLE", "DARK PURPLE", "dark_purple", "dark purple"})
  void testParseEnum(String text) throws TextException {
    assertEquals(ChatColor.DARK_PURPLE, parseEnum(text, ChatColor.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"purple", "dark purp", "dark_p"})
  void testParseEnumFuzzy(String text) throws TextException {
    assertEquals(ChatColor.DARK_PURPLE, parseEnum(text, ChatColor.class, Range.all(), true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "magenta", "checkers", "purple_dark"})
  void testParseEnumInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseEnum(text, ChatColor.class))
            .getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"green", "dark purple", "MAGIC"})
  void testParseEnumOutOfRange(String text) {
    assertEquals(
        "error.outOfRange",
        assertThrows(
                TextException.class,
                () -> parseEnum(text, ChatColor.class, Range.atMost(ChatColor.BLACK), false))
            .getMessage());
  }

  @ParameterizedTest
  @CsvSource({"https://pgm.dev", "file://chinook.db", "mysql://username:password@localhost:3306"})
  void testParseUri(String text) throws TextException, URISyntaxException {
    assertEquals(new URI(text), parseUri(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "https://"})
  void testParseUriInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseUri(text)).getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "notauri"})
  void testParseSqlConnectionInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseSqlConnection(text)).getMessage());
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "&aHello;§aHello",
        "{\"text\":\"World\",\"color\":\"gold\"};§6World",
        "{0}\\n &b&l{2}> {1};{0}\\n §b§l{2}> {1}"
      },
      delimiter = ';')
  void testParseComponentLegacy(String text, String legacyText) {
    assertEquals(legacyText, parseComponentLegacy(text));
  }

  @ParameterizedTest
  @CsvSource({
    "dad8b95c-cf6a-44df-982e-8c8dd70201e0",
    "211fa6b9-5614-340a-a0f0-2a6691b56065",
    "00000000-0000-0000-0000-000000000000"
  })
  void testParseUuid(String text) throws TextException {
    assertEquals(UUID.fromString(text), parseUuid(text));
  }

  @ParameterizedTest
  @CsvSource({"-", "notauuid", "dad8b95ccf6a44df982e8c8dd70201e0"})
  void testParseUuidInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseUuid(text)).getMessage());
  }

  @ParameterizedTest
  @CsvSource({"2000-01-01", "1984-11-11", "2009-09-09"})
  void testParseDate(String text) throws TextException {
    assertEquals(LocalDate.parse(text), parseDate(text));
  }

  @ParameterizedTest
  @CsvSource({"01-01-01", "date", "2021-05"})
  void testParseDateInvalid(String text) {
    assertEquals(
        "error.invalidFormat",
        assertThrows(TextException.class, () -> parseDate(text)).getMessage());
  }
}

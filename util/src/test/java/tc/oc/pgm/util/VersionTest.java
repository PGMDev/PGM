package tc.oc.pgm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public final class VersionTest {

  @ParameterizedTest
  @CsvSource({"1.0, 1,0,0", "3.2.1, 3,2,1", "0.1, 0,1,0"})
  void testToString(String expected, int major, int minor, int patch) {
    assertEquals(expected, new Version(major, minor, patch).toString());
  }

  @ParameterizedTest
  @CsvSource({"-1,0,0", "0,-1,0", "0,0,-1"})
  void testNonNegative(int major, int minor, int patch) {
    assertThrows(IllegalArgumentException.class, () -> new Version(major, minor, patch));
  }

  @ParameterizedTest
  @CsvSource({"0, 1,0,0, 1,0,0", "1, 3,2,1, 3,2,0", "-1, 0,0,0, 0,1,0"})
  void testCompareTo(
      int result, int majorA, int minorA, int patchA, int majorB, int minorB, int patchB) {
    final Version a = new Version(majorA, minorA, patchA);
    final Version b = new Version(majorB, minorB, patchB);

    assertEquals(result, a.compareTo(b));
  }

  @ParameterizedTest
  @CsvSource({"1,1,1"})
  void testEquals(int major, int minor, int patch) {
    final Version a = new Version(major, minor, patch);
    final Version b = new Version(major, minor, patch);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}

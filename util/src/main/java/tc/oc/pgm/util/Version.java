package tc.oc.pgm.util;

import static tc.oc.pgm.util.Assert.assertTrue;

/**
 * A semantic version.
 *
 * @link https://semver.org
 */
public final class Version implements Comparable<Version> {
  private static final String DOT = ".";

  private final int major;
  private final int minor;
  private final int patch;

  /**
   * Creates a semantic version.
   *
   * @param major The major number.
   * @param minor The minor number.
   * @param patch The patch number.
   * @throws IllegalArgumentException If any of the numbers are negative.
   */
  public Version(int major, int minor, int patch) {
    assertTrue(major >= 0, "version major cannot be negative");
    assertTrue(minor >= 0, "version minor cannot be negative");
    assertTrue(patch >= 0, "version patch cannot be negative");

    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  /**
   * Gets whether this version is greater than or equal to another version.
   *
   * @param other Another version.
   * @return If this version >= other version.
   */
  public boolean isNoOlderThan(Version other) {
    return compareTo(other) >= 0;
  }

  /**
   * Gets whether this version is less than another version.
   *
   * @param other Another version.
   * @return If this version < other version.
   */
  public boolean isOlderThan(Version other) {
    return compareTo(other) < 0;
  }

  @Override
  public int compareTo(Version other) {
    int diff = major - other.major;
    if (diff == 0) diff = minor - other.minor;
    if (diff == 0) diff = patch - other.patch;
    return diff;
  }

  @Override
  public int hashCode() {
    return 31 * major ^ 13 * minor ^ 7 * patch;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj instanceof Version) {
      return compareTo((Version) obj) == 0;
    }
    return false;
  }

  @Override
  public String toString() {
    if (patch == 0) {
      return major + DOT + minor;
    } else {
      return major + DOT + minor + DOT + patch;
    }
  }
}

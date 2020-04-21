package tc.oc.pgm.util;

import static com.google.common.base.Preconditions.checkArgument;

public final class Version implements Comparable<Version> {
  private final int major;
  private final int minor;
  private final int patch;

  public Version(int major, int minor, int patch) {
    checkArgument(major >= 0, "version major cannot be negative");
    checkArgument(minor >= 0, "version minor cannot be negative");
    checkArgument(patch >= 0, "version patch cannot be negative");

    this.major = major;
    this.minor = minor;
    this.patch = patch;
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
    if (obj instanceof Version) {
      return compareTo((Version) obj) == 0;
    }
    return false;
  }

  @Override
  public String toString() {
    if (patch == 0) {
      return major + "." + minor;
    } else {
      return major + "." + minor + "." + patch;
    }
  }

  /**
   * Return true if the major versions match and the minor version and patch levels are greater or
   * equal to the given version
   */
  public boolean isNoOlderThan(Version spec) {
    return this.major == spec.major
        && (this.minor > spec.minor || (this.minor == spec.minor && this.patch >= spec.patch));
  }

  /**
   * Return true if the major versions match and the minor version and patch levels are less than
   * the given version
   */
  public boolean isOlderThan(Version spec) {
    return this.major == spec.major
        && (this.minor < spec.minor || (this.minor == spec.minor && this.patch < spec.patch));
  }
}

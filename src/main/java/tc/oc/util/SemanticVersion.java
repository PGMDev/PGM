package tc.oc.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.stream.Stream;

public class SemanticVersion implements Comparable<SemanticVersion>, Iterable<Integer> {

  private final int major;
  private final int minor;
  private final int patch;

  public SemanticVersion(int major, int minor, int patch) {
    checkArgument(major >= 0, "version major cannot be negative");
    checkArgument(minor >= 0, "version minor cannot be negative");
    checkArgument(patch >= 0, "version patch cannot be negative");

    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  @Override
  public Iterator<Integer> iterator() {
    return Stream.of(major, minor, patch).iterator();
  }

  @Override
  public int compareTo(SemanticVersion obj) {
    final Iterator<Integer> other = obj.iterator();
    for (int i : this) {
      final int compare = Integer.compare(i, other.next());
      if (compare != 0) {
        return compare;
      }
    }
    return 0;
  }

  @Override
  public int hashCode() {
    return 31 * major ^ 13 * minor ^ 7 * patch;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SemanticVersion) {
      return compareTo((SemanticVersion) obj) == 0;
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
   * Return true if the major versions match and the minor version and patch levels are less or
   * equal to the given version
   */
  public boolean isNoNewerThan(SemanticVersion spec) {
    return this.major == spec.major
        && (this.minor < spec.minor || (this.minor == spec.minor && this.patch <= spec.patch));
  }

  /**
   * Return true if the major versions match and the minor version and patch levels are greater than
   * the given version
   */
  public boolean isNewerThan(SemanticVersion spec) {
    return this.major == spec.major
        && (this.minor > spec.minor || (this.minor == spec.minor && this.patch > spec.patch));
  }

  /**
   * Return true if the major versions match and the minor version and patch levels are greater or
   * equal to the given version
   */
  public boolean isNoOlderThan(SemanticVersion spec) {
    return this.major == spec.major
        && (this.minor > spec.minor || (this.minor == spec.minor && this.patch >= spec.patch));
  }

  /**
   * Return true if the major versions match and the minor version and patch levels are less than
   * the given version
   */
  public boolean isOlderThan(SemanticVersion spec) {
    return this.major == spec.major
        && (this.minor < spec.minor || (this.minor == spec.minor && this.patch < spec.patch));
  }
}

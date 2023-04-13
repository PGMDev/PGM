package tc.oc.pgm.util.range;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

/** Some {@link Range} utils ripped from ProjectAres :) */
public class Ranges {

  public static boolean isBounded(Range<?> range) {
    return range.hasLowerBound() && range.hasUpperBound();
  }

  public static void assertLowerBound(Range<?> range) {
    if (!range.hasLowerBound()) {
      throw new IllegalArgumentException("Range has no lower bound");
    }
  }

  public static void assertUpperBound(Range<?> range) {
    if (!range.hasLowerBound()) {
      throw new IllegalArgumentException("Range has no upper bound");
    }
  }

  public static int needMinimum(Range<Integer> range) {
    assertLowerBound(range);
    return range.lowerBoundType() == BoundType.CLOSED
        ? range.lowerEndpoint()
        : range.lowerEndpoint() + 1;
  }

  public static int needMaximum(Range<Integer> range) {
    assertUpperBound(range);
    return range.upperBoundType() == BoundType.CLOSED
        ? range.upperEndpoint()
        : range.upperEndpoint() - 1;
  }

  public static Range<Integer> toClosed(Range<Integer> range) {
    return range.lowerBoundType() == BoundType.CLOSED && range.upperBoundType() == BoundType.CLOSED
        ? range
        : Range.closed(needMinimum(range), needMaximum(range));
  }
}

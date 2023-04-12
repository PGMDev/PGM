package tc.oc.pgm.util.compose;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.loot.WeightedRandomChooser;
import tc.oc.pgm.util.range.Ranges;

public class Any<T> implements Composition<T> {

  public static class Option<T> {
    private final double weight;
    private final Filter filter;
    private final Composition<T> element;

    public Option(double weight, Filter filter, Composition<T> element) {
      this.weight = weight;
      this.filter = filter;
      this.element = element;
    }
  }

  private int countMaxAmount;
  private final int countOffset;
  private final boolean unique;
  private final List<Option<T>> options;
  private final double totalWeight;

  public Any(Range<Integer> count, boolean unique, Iterable<Option<T>> choices) {
    // Range is enforced to be bounded in constructor
    count = Ranges.toClosed(count);
    this.countOffset = count.lowerEndpoint();
    // If the range is a singleton we need to manually set this to 1 since
    // lower and upper endpoint will be identical
    this.countMaxAmount = Math.max(count.upperEndpoint() - count.lowerEndpoint(), 1);
    this.unique = unique;
    this.options = ImmutableList.copyOf(choices);
    this.totalWeight = this.options.stream().mapToDouble(c -> c.weight).sum();
  }

  @Override
  public Stream<T> elements(Query query) {
    if (totalWeight <= 0) return Stream.empty();

    final WeightedRandomChooser<Option<T>> chooser = new WeightedRandomChooser<>();
    for (Option<T> option : options) {
      if (option.filter.query(query).isAllowed()) {
        chooser.add(option, option.weight);
      }
    }

    final Random random = new Random();
    Stream<T> result = Stream.empty();

    for (int count = random.nextInt(this.countMaxAmount + 1) + this.countOffset;
        count > 0 && !chooser.isEmpty();
        count--) {
      final Option<T> option = chooser.choose(random);
      result = Stream.concat(result, option.element.elements(query));
      if (unique) chooser.remove(option);
    }
    return result;
  }
}

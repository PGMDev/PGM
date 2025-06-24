package tc.oc.pgm.util.compose;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.loot.WeightedRandomChooser;
import tc.oc.pgm.util.range.Ranges;

public class Any<T> implements Composition<T> {

  private static final Random RANDOM = new Random();

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

  private final Range<Integer> range;
  private final boolean unique;
  private final List<Option<T>> options;
  private final double totalWeight;

  public Any(Range<Integer> count, boolean unique, Iterable<Option<T>> choices) {
    this.range = Ranges.toClosed(count);
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

    int picks = random(range.lowerEndpoint(), range.upperEndpoint());
    List<Stream<T>> result = new ArrayList<>(picks);

    for (int i = picks; i > 0 && !chooser.isEmpty(); i--) {
      final Option<T> option = chooser.choose(RANDOM);
      result.add(option.element.elements(query));
      if (unique) chooser.remove(option);
    }
    return result.stream().flatMap(Function.identity());
  }

  private int random(int min, int max) {
    return min >= max ? min : min + RANDOM.nextInt(max + 1 - min);
  }
}

package tc.oc.pgm.loot;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import tc.oc.pgm.util.text.TextException;

public class WeightedRandomChooser<T> {

  private final Map<T, Double> options = new HashMap<>();
  private double totalWeight = 0;

  public WeightedRandomChooser() {}

  /**
   * Choose an item at random using the given generator. The probability of each item being chosen
   * is proportional its weight. The choice will be consistent with regard to the state of the
   * generator. Beyond that, the choice mechanism is undefined.
   *
   * @param random A Random instance
   * @return An item passed to the constructor
   * @throws NoSuchElementException if this chooser is empty
   */
  public T choose(Random random) {
    double n = random.nextDouble();

    if (this.isEmpty()) {
      throw new NoSuchElementException("No choices");
    }

    n *= totalWeight;
    for (Map.Entry<T, Double> option : options.entrySet()) {
      if (n < option.getValue()) {
        return option.getKey();
      } else {
        n -= option.getValue();
      }
    }

    throw TextException.exception("error.unknown"); // Should be impossible
  }

  public void add(T choice, double weight) {
    if (weight > 0) {
      totalWeight += weight;
      options.put(choice, weight);
    }
  }

  public void remove(T choice) {
    final Double weight = options.remove(choice);
    if (weight != null) {
      totalWeight -= weight;
    }
  }

  public void addAll(Map<T, Double> weightedChoices) {
    weightedChoices.forEach(this::add);
  }

  public void removeAll(Iterable<T> choices) {
    choices.forEach(this::remove);
  }

  public boolean isEmpty() {
    return this.options.isEmpty();
  }
}

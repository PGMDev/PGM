package tc.oc.pgm.api;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.VariableDurationImpl;
import tc.oc.pgm.util.text.TextException;

/** A duration that can vary based on the number of players and the length of the match. */
public interface VariableDuration {
  /**
   * Gets the duration for the given match.
   *
   * @param match The match.
   * @return The duration.
   */
  Duration getDuration(Match match);

  /**
   * Gets the default duration.
   *
   * @return The default duration.
   */
  Duration getDefault();

  /**
   * Parses a variable duration from the given object.
   *
   * @param object The object to parse (String or ConfigurationSection).
   * @param def The default duration string if the object is null.
   * @return The parsed variable duration.
   * @throws TextException If the object is not a valid variable duration.
   */
  static VariableDuration parse(Object object, String def) throws TextException {
    return switch (object) {
      case ConfigurationSection cs -> VariableDurationImpl.parse(cs);
      case String str -> VariableDurationImpl.of(parseDuration(str));
      case null, default -> VariableDurationImpl.of(parseDuration(def));
    };
  }

  /**
   * Creates a variable duration that always returns the same value.
   *
   * @param duration The duration.
   * @return The variable duration.
   */
  static VariableDuration constant(Duration duration) {
    return VariableDurationImpl.of(duration);
  }
}

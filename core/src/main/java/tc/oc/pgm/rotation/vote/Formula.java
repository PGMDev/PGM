package tc.oc.pgm.rotation.vote;

import java.util.function.ToDoubleFunction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import tc.oc.pgm.api.PGM;

/** Implementation of different formulas configurable & composable via config sections. */
public interface Formula extends ToDoubleFunction<Formula.Context> {

  class Context {
    double score;
    double sameGamemode;
    double mapsize;
    double players;

    public Context(double score, double sameGamemode, double mapsize, double players) {
      this.score = score;
      this.sameGamemode = sameGamemode;
      this.mapsize = mapsize;
      this.players = players;
    }
  }

  Formula ZERO = constant(0);
  Formula ONE = constant(1);
  Formula POSITIVE_INFINITY = constant(Double.POSITIVE_INFINITY);
  Formula NEGATIVE_INFINITY = constant(Double.NEGATIVE_INFINITY);

  static Formula ofRequired(ConfigurationSection parent, String child)
      throws InvalidConfigurationException {
    Formula result = of(parent, child, null);
    if (result == null)
      throw new InvalidConfigurationException(
          "Child '" + child + "' in '" + parent.getCurrentPath() + " is required");
    return result;
  }

  static Formula of(ConfigurationSection parent, String child, Formula fallback)
      throws InvalidConfigurationException {
    if (parent == null) return fallback;
    Object obj = parent.get(child);

    if (obj == null) return fallback;
    else if (obj instanceof Number) return constant(((Number) obj).doubleValue());
    else if (obj instanceof String) return variable((String) obj);
    else if (obj instanceof ConfigurationSection) return of((ConfigurationSection) obj);
    else
      throw new InvalidConfigurationException(
          "Child '"
              + child
              + "' in '"
              + parent.getCurrentPath()
              + " must be null, double, or config section");
  }

  static Formula of(ConfigurationSection config) throws InvalidConfigurationException {
    String type = config.getString("type").toLowerCase();
    switch (type) {
      case "constant":
        return constant(config.getDouble("value"));
      case "variable":
        return variable(config.getString("value"));
      case "sum":
        return sum(
            ofRequired(config, "a"),
            ofRequired(config, "b"),
            of(config, "c", ZERO),
            of(config, "d", ZERO));
      case "sub":
        return sub(ofRequired(config, "a"), ofRequired(config, "b"));
      case "mul":
        return mul(
            ofRequired(config, "a"),
            ofRequired(config, "b"),
            of(config, "c", ONE),
            of(config, "d", ONE));
      case "pow":
        return pow(ofRequired(config, "value"), ofRequired(config, "exponent"));
      case "bound":
        return bound(
            ofRequired(config, "value"),
            of(config, "min", NEGATIVE_INFINITY),
            of(config, "max", POSITIVE_INFINITY));
      case "linear":
        return linear(
            ofRequired(config, "value"), of(config, "slope", ZERO), of(config, "offset", ZERO));
      case "quadratic":
        return quadratic(
            ofRequired(config, "value"),
            of(config, "arc", ZERO),
            of(config, "slope", ZERO),
            of(config, "offset", ZERO));
      case "tanh":
        return tanh(ofRequired(config, "value"));
      default:
        PGM.get()
            .getLogger()
            .severe("Invalid formula type for " + config.getCurrentPath() + ": '" + type + "'");
    }
    return null;
  }

  static Formula constant(double d) {
    return x -> d;
  }

  static Formula variable(String str) throws InvalidConfigurationException {
    switch (str) {
      case "score":
        return in -> in.score;
      case "same_gamemode":
        return in -> in.sameGamemode;
      case "mapsize":
        return in -> in.mapsize;
      case "players":
        return in -> in.players;
      default:
        throw new InvalidConfigurationException("Unknown variable type " + str);
    }
  }

  static Formula sum(Formula a, Formula b, Formula c, Formula d) {
    return x -> a.applyAsDouble(x) + b.applyAsDouble(x) + c.applyAsDouble(x) + d.applyAsDouble(x);
  }

  static Formula sub(Formula a, Formula b) {
    return x -> a.applyAsDouble(x) - b.applyAsDouble(x);
  }

  static Formula mul(Formula a, Formula b, Formula c, Formula d) {
    return x -> a.applyAsDouble(x) * b.applyAsDouble(x) * c.applyAsDouble(x) * d.applyAsDouble(x);
  }

  static Formula pow(Formula value, Formula exponent) {
    return x -> Math.pow(value.applyAsDouble(x), exponent.applyAsDouble(x));
  }

  static Formula bound(Formula value, Formula min, Formula max) {
    return x ->
        Math.min(max.applyAsDouble(x), Math.max(min.applyAsDouble(x), value.applyAsDouble(x)));
  }

  static Formula linear(Formula val, Formula a, Formula b) {
    return x -> a.applyAsDouble(x) * val.applyAsDouble(x) + b.applyAsDouble(x);
  }

  static Formula quadratic(Formula val, Formula a, Formula b, Formula c) {
    return x -> {
      double v = val.applyAsDouble(x);
      return a.applyAsDouble(x) * v * v + b.applyAsDouble(x) * v + c.applyAsDouble(x);
    };
  }

  static Formula tanh(Formula val) {
    return x -> Math.tanh(val.applyAsDouble(x));
  }
}

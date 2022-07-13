package tc.oc.pgm.rotation.vote;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.bukkit.configuration.InvalidConfigurationException;

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

    public Map<String, Double> asMap() {
      return ImmutableMap.of(
          "score", score,
          "same_gamemode", sameGamemode,
          "mapsize", mapsize,
          "players", players);
    }
  }

  Function BOUND =
      new Function("bound", 3) {
        @Override
        public double apply(double... doubles) {
          double val = doubles[0];
          double min = doubles[1];
          double max = doubles[2];
          return Math.max(min, Math.min(val, max));
        }
      };

  static Formula of(String expression, Formula fallback) throws InvalidConfigurationException {
    if (expression == null) return fallback;

    try {
      Expression exp =
          new ExpressionBuilder(expression)
              .variables(new Context(0, 0, 0, 0).asMap().keySet())
              .function(BOUND)
              .build();

      return context -> exp.setVariables(context.asMap()).evaluate();
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigurationException(e);
    }
  }
}

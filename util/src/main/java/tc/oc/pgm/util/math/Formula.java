package tc.oc.pgm.util.math;

import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ExpressionContext;
import net.objecthunter.exp4j.function.Function;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface Formula<T> extends ToDoubleFunction<T> {
  /**
   * Create a formula for a config, if there's a misconfiguration it logs and uses fallback
   *
   * @param expression The expression to parse
   * @param variables The set of available variables in the formula
   * @param fallback A fallback if no value is defined or parsing fails
   * @return The formula if it parsed correctly, fallback if anything goes wrong
   * @param <T> Type of expression context to use
   */
  static <T extends ExpressionContext> Formula<T> of(
      String expression, Set<String> variables, Formula<T> fallback) {
    if (expression == null) return fallback;

    try {
      return Formula.of(expression, ContextFactory.ofStatic(variables));
    } catch (IllegalArgumentException e) {
      BukkitUtils.getPlugin()
          .getLogger()
          .log(Level.SEVERE, "Failed to load formula '" + expression + "' using fallback", e);
      return fallback;
    }
  }

  static <T> Formula<T> of(String expression, ContextFactory<T> context)
      throws IllegalArgumentException {
    Expression exp = new ExpressionBuilder(expression)
        .variables(context.getVariables())
        .functions(AddedFunctions.ALL)
        .functions(context.getArrays().stream()
            .map(str -> new Function(str, 1) {
              @Override
              public double apply(double... doubles) {
                throw new UnsupportedOperationException(
                    "Cannot get array value without replacement!");
              }
            })
            .collect(Collectors.toList()))
        .build();

    return new ExpFormula<>(exp, context);
  }

  default <R> Formula<R> map(java.util.function.Function<R, T> mapper) {
    return v -> apply(mapper.apply(v));
  }

  /** Shorthand for {@link #applyAsDouble} */
  default double apply(T value) {
    return applyAsDouble(value);
  }

  class ExpFormula<T> implements Formula<T> {
    private final Expression expression;
    private final ContextFactory<T> context;

    private ExpFormula(Expression expression, ContextFactory<T> context) {
      this.expression = expression;
      this.context = context;
    }

    @Override
    public double applyAsDouble(T value) {
      return expression.setExpressionContext(context.withContext(value)).evaluate();
    }
  }

  interface ContextFactory<T> {
    Set<String> getVariables();

    Set<String> getArrays();

    ExpressionContext withContext(T t);

    static <T extends ExpressionContext> ContextFactory<T> ofStatic(Set<String> variables) {
      return new ContextFactory<>() {
        @Override
        public ExpressionContext withContext(T t) {
          return t;
        }

        @Override
        public Set<String> getVariables() {
          return variables;
        }

        @Override
        public Set<String> getArrays() {
          return Set.of();
        }
      };
    }
  }
}

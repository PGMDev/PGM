package tc.oc.pgm.util.math;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

public interface Formula<T> extends ToDoubleFunction<T> {

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

  Function RANDOM =
      new Function("random", 0) {
        @Override
        public double apply(double... doubles) {
          return Math.random();
        }
      };

  static <T extends Supplier<Map<String, Double>>> Formula<T> of(
      String expression, Set<String> variables, Formula<T> fallback)
      throws IllegalArgumentException {
    if (expression == null) return fallback;

    return Formula.of(expression, variables, T::get);
  }

  static <T> Formula<T> of(String expression, Set<String> variables, ContextBuilder<T> context)
      throws IllegalArgumentException {

    Expression exp =
        new ExpressionBuilder(expression).variables(variables).functions(BOUND, RANDOM).build();

    return new ExpFormula<>(exp, context);
  }

  class ExpFormula<T> implements Formula<T> {
    private final Expression expression;
    private final ContextBuilder<T> context;

    private ExpFormula(Expression expression, ContextBuilder<T> context) {
      this.expression = expression;
      this.context = context;
    }

    @Override
    public double applyAsDouble(T value) {
      return expression.setVariables(context.getVariables(value)).evaluate();
    }
  }

  @FunctionalInterface
  interface ContextBuilder<T> {
    Map<String, Double> getVariables(T t);
  }
}

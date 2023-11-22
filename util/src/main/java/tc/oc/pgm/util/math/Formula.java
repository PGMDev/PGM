package tc.oc.pgm.util.math;

import java.util.Collections;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ExpressionContext;
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

  Function MAX =
      new Function("max") {
        @Override
        public double apply(double... doubles) {
          double max = doubles[0];
          for (int i = 1; i < doubles.length; i++) max = Math.max(max, doubles[i]);
          return max;
        }

        @Override
        public boolean isValidArgCount(int count) {
          return count >= 1;
        }
      };

  Function MIN =
      new Function("min") {
        @Override
        public double apply(double... doubles) {
          double min = doubles[0];
          for (int i = 1; i < doubles.length; i++) min = Math.min(min, doubles[i]);
          return min;
        }

        @Override
        public boolean isValidArgCount(int count) {
          return count >= 1;
        }
      };

  static <T> Formula<T> of(String expression, ContextFactory<T> context, Formula<T> fallback)
      throws IllegalArgumentException {
    if (expression == null) return fallback;

    return Formula.of(expression, context);
  }

  static <T> Formula<T> of(String expression, ContextFactory<T> context)
      throws IllegalArgumentException {
    Expression exp =
        new ExpressionBuilder(expression)
            .variables(context.getVariables())
            .functions(BOUND, RANDOM, MAX, MIN)
            .functions(
                context.getArrays().stream()
                    .map(
                        str ->
                            new Function(str, 1) {
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
      return of(variables, Collections.emptySet(), t -> t);
    }

    static <T> ContextFactory<T> of(
        Set<String> variables,
        Set<String> arrays,
        java.util.function.Function<T, ExpressionContext> builder) {
      return new ContextFactory<T>() {
        @Override
        public ExpressionContext withContext(T t) {
          return builder.apply(t);
        }

        @Override
        public Set<String> getVariables() {
          return variables;
        }

        @Override
        public Set<String> getArrays() {
          return arrays;
        }
      };
    }
  }
}

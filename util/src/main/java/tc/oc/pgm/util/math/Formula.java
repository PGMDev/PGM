package tc.oc.pgm.util.math;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ExpressionContext;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.shuntingyard.ShuntingYard;
import net.objecthunter.exp4j.tokenizer.FunctionToken;
import net.objecthunter.exp4j.tokenizer.VariableToken;
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

  static <T> ExpFormula<T> of(String expression, ContextFactory<T> context)
      throws IllegalArgumentException {
    Expression exp = new ExpressionBuilder(expression)
        .variables(context.getVariables())
        .functions(AddedFunctions.ALL)
        .functions(
            context.getArrays().stream().<Function>map(ArrayPlaceholder::new).toList())
        .build();

    return new ExpFormula<>(exp, context);
  }

  static Set<String> getUsedVariables(String expr, ContextFactory<?> context)
      throws IllegalArgumentException {
    var fn = new HashMap<>(AddedFunctions.BY_NAME);
    context.getArrays().forEach(arr -> fn.put(arr, new ArrayPlaceholder(arr)));

    var result = new HashSet<String>();
    for (var token : ShuntingYard.convertToRPN(expr, fn, Map.of(), context.getVariables(), false)) {
      if (token instanceof VariableToken vt) result.add(vt.getName());
      else if (token instanceof FunctionToken ft) {
        if (ft.getFunction() instanceof ArrayPlaceholder a) result.add(a.getName());
      }
    }
    return result;
  }

  default <R> Formula<R> map(java.util.function.Function<R, T> mapper) {
    return v -> apply(mapper.apply(v));
  }

  /** Shorthand for {@link #applyAsDouble} */
  default double apply(T value) {
    return applyAsDouble(value);
  }

  record ExpFormula<T>(Expression expression, ContextFactory<T> context) implements Formula<T> {
    @Override
    public double applyAsDouble(T value) {
      return expression.setExpressionContext(context.withContext(value)).evaluate();
    }
  }

  class ArrayPlaceholder extends Function {
    private ArrayPlaceholder(String name) {
      super(name, 1);
    }

    @Override
    public double apply(double... doubles) {
      throw new UnsupportedOperationException(
          "Function can only be ran after replacement with a context");
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

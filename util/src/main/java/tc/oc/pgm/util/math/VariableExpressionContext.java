package tc.oc.pgm.util.math;

import java.util.Collections;
import java.util.Set;
import net.objecthunter.exp4j.ExpressionContext;
import net.objecthunter.exp4j.function.Function;

public interface VariableExpressionContext extends ExpressionContext {

  @Override
  default Set<String> getFunctions() {
    return Collections.emptySet();
  }

  @Override
  default Function getFunction(String s) {
    return null;
  }
}

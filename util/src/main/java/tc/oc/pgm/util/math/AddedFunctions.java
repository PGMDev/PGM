package tc.oc.pgm.util.math;

import java.util.List;
import net.objecthunter.exp4j.function.Function;

class AddedFunctions {
  public static final List<Function> ALL = List.of(
      new Function("bound", 3) {
        @Override
        public double apply(double... doubles) {
          double val = doubles[0];
          double min = doubles[1];
          double max = doubles[2];
          return Math.max(min, Math.min(val, max));
        }
      },
      new Function("random", 0) {
        @Override
        public double apply(double... doubles) {
          return Math.random();
        }
      },
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
      },
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
      });
}

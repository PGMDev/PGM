package tc.oc.pgm.util;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

public record Pair<L, R>(L left, R right) {

  public static <L, R> Pair<L, R> of(L left, R right) {
    return new Pair<>(left, right);
  }

  @Deprecated
  public L getLeft() {
    return left();
  }

  @Deprecated
  public R getRight() {
    return right();
  }

  @Override
  public @NonNull String toString() {
    return "Pair{" + "left=" + left + ", right=" + right + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }
}

package tc.oc.pgm.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface Result<T, E extends Throwable> permits Result.Ok, Result.Err {
  T value() throws E;

  E error();

  boolean isOk();

  boolean isError();

  static <T, E extends Throwable> Result<T, E> ok(T value) {
    return new Ok<>(value);
  }

  static <T, E extends Throwable> Result<T, E> err(E error) {
    return new Err<>(error);
  }

  record Ok<T, E extends Throwable>(T value) implements Result<T, E> {
    @Override
    public E error() {
      throw new IllegalStateException("No error present in Ok result");
    }

    @Override
    public boolean isOk() {
      return true;
    }

    @Override
    public boolean isError() {
      return false;
    }
  }

  record Err<T, E extends Throwable>(E error) implements Result<T, E> {
    @Override
    public T value() throws E {
      throw this.error;
    }

    @Override
    public boolean isOk() {
      return false;
    }

    @Override
    public boolean isError() {
      return true;
    }
  }
}

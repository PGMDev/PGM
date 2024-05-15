package tc.oc.pgm.command.parsers;

import java.util.function.Function;
import org.incendo.cloud.parser.ArgumentParseResult;

public interface ParseUtils {

  default <I, R> ArgumentParseResult<R> flatMap(
      ArgumentParseResult<I> input, Function<I, ArgumentParseResult<R>> mapper) {
    return input
        .parsedValue()
        .map(mapper)
        .orElseGet(() -> ArgumentParseResult.failure(input.failure().get()));
  }

  default <I, R> ArgumentParseResult<R> map(ArgumentParseResult<I> input, Function<I, R> mapper) {
    return input
        .parsedValue()
        .map(mapper)
        .map(ArgumentParseResult::success)
        .orElseGet(() -> ArgumentParseResult.failure(input.failure().get()));
  }
}

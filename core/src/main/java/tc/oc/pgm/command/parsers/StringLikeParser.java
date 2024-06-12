package tc.oc.pgm.command.parsers;

import io.leangen.geantyref.TypeToken;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserParameters;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.StringUtils;

public abstract class StringLikeParser<S, T> implements ArgumentParser<S, T> {

  private final ArgumentParser<S, String> stringParser;
  protected final ParserParameters options;

  public StringLikeParser(CommandManager<S> manager, ParserParameters options) {
    this.options = options;
    this.stringParser =
        manager
            .parserRegistry()
            .createParser(TypeToken.get(String.class), options)
            .orElseThrow(IllegalStateException::new);
  }

  @Override
  public @NotNull ArgumentParseResult<T> parse(
      @NotNull CommandContext<S> context, @NotNull CommandInput args) {
    return stringParser
        .parse(context, args)
        .flatMapSuccess(text -> parse(context, StringUtils.suggestionToText(text)));
  }

  public abstract ArgumentParseResult<T> parse(
      @NotNull CommandContext<S> context, @NotNull String text);
}

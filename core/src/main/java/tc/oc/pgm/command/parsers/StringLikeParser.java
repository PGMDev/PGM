package tc.oc.pgm.command.parsers;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import io.leangen.geantyref.TypeToken;
import java.util.Queue;
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
      @NotNull CommandContext<S> context, @NotNull Queue<String> args) {
    return stringParser
        .parse(context, args)
        .flatMapParsedValue(text -> parse(context, StringUtils.suggestionToText(text)));
  }

  public abstract ArgumentParseResult<T> parse(
      @NotNull CommandContext<S> context, @NotNull String text);
}

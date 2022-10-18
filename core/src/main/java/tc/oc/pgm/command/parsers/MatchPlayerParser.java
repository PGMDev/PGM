package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Players;

public final class MatchPlayerParser implements ArgumentParser<CommandSender, MatchPlayer> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull MatchPlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context,
      @NotNull Queue<@NotNull String> inputQueue) {
    final String input = inputQueue.peek();

    if (input == null) {
      return failure(new NoInputProvidedException(DurationParser.class, context));
    }

    CommandSender sender = context.getSender();
    MatchPlayer mp = Players.getMatchPlayer(sender, input);

    return mp != null ? success(mp) : failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> suggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull String input) {
    CommandSender sender = context.getSender();

    return Players.getPlayerNames(sender, input);
  }
}

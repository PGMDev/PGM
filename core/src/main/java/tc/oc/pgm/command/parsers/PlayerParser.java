package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.Players;

public final class PlayerParser
    implements ArgumentParser<CommandSender, Player>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull Player> parse(
      @NotNull CommandContext<@NotNull CommandSender> context, @NotNull CommandInput inputQueue) {
    final String input = inputQueue.readString();

    CommandSender sender = context.sender();
    Player player;

    if (input.equals(CURRENT)) {
      if (!(context.sender() instanceof Player)) return failure(playerOnly());
      player = (Player) context.sender();
    } else {
      player = Players.getPlayer(sender, input);
    }

    return player != null ? success(player) : failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    CommandSender sender = context.sender();

    return Players.getPlayerNames(sender, input.readString());
  }
}

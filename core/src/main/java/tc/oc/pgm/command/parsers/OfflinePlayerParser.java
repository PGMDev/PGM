package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class OfflinePlayerParser
    implements ArgumentParser<CommandSender, OfflinePlayer>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull OfflinePlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context, @NotNull CommandInput inputQueue) {
    final String input = inputQueue.peekString();

    CommandSender sender = context.sender();
    OfflinePlayer player;

    if (input.equals(CURRENT)) {
      if (!(context.sender() instanceof Player)) return failure(playerOnly());
      player = (Player) context.sender();
    } else {
      player = Players.getPlayer(sender, input);

      if (player == null) {
        try {
          UUID uuid = TextParser.parseUuid(input);
          player = Bukkit.getOfflinePlayer(uuid);
        } catch (TextException e) {
          return failure(e);
        }
      }
    }

    if (player != null) {
      inputQueue.readString();
      return success(player);
    }
    return failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    CommandSender sender = context.sender();

    return Players.getPlayerNames(sender, input.readString());
  }
}

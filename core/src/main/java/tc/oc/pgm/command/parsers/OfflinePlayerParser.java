package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class OfflinePlayerParser implements ArgumentParser<CommandSender, OfflinePlayer> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull OfflinePlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context,
      @NotNull Queue<@NotNull String> inputQueue) {
    final String input = inputQueue.peek();

    if (input == null) {
      return failure(new NoInputProvidedException(OfflinePlayerParser.class, context));
    }

    CommandSender sender = context.getSender();
    OfflinePlayer player;

    if (input.equals(CURRENT)) {
      if (!(context.getSender() instanceof Player)) return failure(playerOnly());
      player = (Player) context.getSender();
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
      inputQueue.poll();
      return success(player);
    }
    return failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> suggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull String input) {
    CommandSender sender = context.getSender();

    return Players.getPlayerNames(sender, input);
  }
}

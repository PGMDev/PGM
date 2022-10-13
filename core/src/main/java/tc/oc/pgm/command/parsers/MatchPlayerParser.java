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
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.StringUtils;

public class MatchPlayerParser implements ArgumentParser<CommandSender, MatchPlayer> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull MatchPlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context,
      @NotNull Queue<@NotNull String> inputQueue) {
    final String input = inputQueue.peek();

    if (input == null) {
      return failure(new NoInputProvidedException(DurationParser.class, context));
    }

    CommandSender sender = context.getSender();

    Player player =
        StringUtils.bestFuzzyMatch(
            input,
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> Players.shouldShow(sender, p))
                .iterator(),
            p -> Players.getVisibleName(sender, p),
            0.75);

    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);

    return mp != null ? success(mp) : failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> suggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull String input) {
    CommandSender sender = context.getSender();

    return Bukkit.getOnlinePlayers().stream()
        .filter(p -> Players.shouldShow(sender, p))
        .map(p -> Players.getVisibleName(sender, p))
        .filter(n -> input.toLowerCase().startsWith(n.toLowerCase()))
        .collect(Collectors.toList());
  }
}

package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;

public final class PartyParser extends StringLikeParser<CommandSender, Party> {

  public PartyParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
  }

  @Override
  public ArgumentParseResult<Party> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    final Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match == null) return failure(exception("command.onlyPlayers"));

    if (text.equalsIgnoreCase("obs")) return success(match.getDefaultParty());

    return TeamParser.getTeam(match, text).mapParsedValue(team -> (Party) team);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    return Stream.concat(Stream.of("obs"), TeamParser.getTeams(context.getSender()))
        .collect(Collectors.toList());
  }
}

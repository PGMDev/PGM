package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;

public final class PlayerClassParser extends StringLikeParser<CommandSender, PlayerClass> {

  public PlayerClassParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
  }

  @Override
  public ArgumentParseResult<PlayerClass> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match == null) return failure(playerOnly());

    ClassMatchModule classes = match.getModule(ClassMatchModule.class);
    if (classes == null) return failure(exception("match.class.notEnabled"));

    PlayerClass playerClass = StringUtils.bestFuzzyMatch(text, classes.getClasses());
    if (playerClass == null) return failure(exception("match.class.notFound"));

    return ArgumentParseResult.success(playerClass);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match == null) return Collections.emptyList();

    List<String> inputQueue = context.get(CommandKeys.INPUT_QUEUE);
    String text = StringUtils.getText(inputQueue);
    String mustKeep = StringUtils.getMustKeepText(inputQueue);

    return match.moduleOptional(ClassMatchModule.class).map(ClassMatchModule::getClasses)
        .orElse(Collections.emptySet()).stream()
        .map(PlayerClass::getName)
        .filter(name -> LiquidMetal.match(name, text))
        .map(name -> StringUtils.getSuggestion(name, mustKeep))
        .collect(Collectors.toList());
  }
}

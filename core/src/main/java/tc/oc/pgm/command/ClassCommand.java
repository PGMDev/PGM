package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.specifier.FlagYielding;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.text.TextTranslations;

public final class ClassCommand {

  @CommandMethod("class|selectclass|c|cl [class]")
  @CommandDescription("Select your class")
  public void classSelect(
      Match match,
      MatchPlayer player,
      @Argument(value = "class", suggestions = "pgm-classes") @FlagYielding String query) {
    final ClassMatchModule classes = getClasses(match);
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    if (query == null) {
      player.sendMessage(
          translatable("match.class.current", NamedTextColor.GREEN)
              .append(space())
              .append(text(currentClass.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
      player.sendMessage(translatable("match.class.view", NamedTextColor.GOLD));
    } else {
      final PlayerClass newClass =
          StringUtils.bestFuzzyMatch(StringUtils.suggestionToText(query), classes.getClasses());

      if (newClass == null) {
        throw exception("match.class.notFound");
      }

      try {
        classes.setPlayerClass(player.getId(), newClass);
      } catch (IllegalStateException e) {
        throw exception("match.class.sticky");
      }

      player.sendMessage(
          translatable(
              "match.class.ok",
              NamedTextColor.GREEN,
              text(newClass.getName(), NamedTextColor.GOLD, TextDecoration.UNDERLINED)));
      if (player.isParticipating()) {
        player.sendMessage(translatable("match.class.queue", NamedTextColor.GREEN));
      }
    }
  }

  @Suggestions("pgm-classes")
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match == null) return Collections.emptyList();

    List<String> inputQueue = context.get(CommandGraph.INPUT_QUEUE);
    String text = StringUtils.getText(inputQueue);
    String mustKeep = StringUtils.getMustKeepText(inputQueue);

    return match.moduleOptional(ClassMatchModule.class).map(ClassMatchModule::getClasses)
        .orElse(Collections.emptySet()).stream()
        .map(PlayerClass::getName)
        .filter(name -> LiquidMetal.match(name, text))
        .map(name -> StringUtils.getSuggestion(name, mustKeep))
        .collect(Collectors.toList());
  }

  @CommandMethod("classlist|classes|listclasses|cls")
  @CommandDescription("List all available classes")
  public void classList(Match match, MatchPlayer player) {
    final ClassMatchModule classes = getClasses(match);
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    player.sendMessage(
        text(
            LegacyFormatUtils.dashedChatMessage(
                ChatColor.GOLD
                    + TextTranslations.translate("match.class.title", player.getBukkit()),
                "-",
                ChatColor.RED.toString())));
    int i = 1;
    for (PlayerClass cls : classes.getClasses()) {
      TextComponent.Builder result = text().append(text(i++ + ". "));

      NamedTextColor color;

      if (cls == currentClass) {
        color = NamedTextColor.GOLD;
      } else if (cls.canUse(player.getBukkit())) {
        color = NamedTextColor.GREEN;
      } else {
        color = NamedTextColor.RED;
      }

      result.append(
          text(cls.getName(), color).decoration(TextDecoration.UNDERLINED, cls == currentClass));

      if (cls.getDescription() != null) {
        result.append(text(" - ", NamedTextColor.DARK_PURPLE)).append(text(cls.getDescription()));
      }

      player.sendMessage(result.build());
    }
  }

  private ClassMatchModule getClasses(Match match) {
    final ClassMatchModule classes = match.getModule(ClassMatchModule.class);
    if (classes == null) {
      throw new CommandExecutionException(exception("match.class.notEnabled"));
    }
    return classes;
  }
}

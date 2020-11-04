package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Text;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;

public final class ClassCommand {

  @Command(
      aliases = {"class", "selectclass", "c", "cl"},
      desc = "Select your class")
  public void classSelect(Match match, MatchPlayer player, @Nullable @Text String query) {
    final ClassMatchModule classes = getClasses(match);
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    if (query == null) {
      player.sendMessage(
          Component.translatable("match.class.current", NamedTextColor.GREEN)
              .append(Component.space())
              .append(text(currentClass.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
      player.sendMessage(Component.translatable("match.class.view", NamedTextColor.GOLD));
    } else {
      final PlayerClass newClass = StringUtils.bestFuzzyMatch(query, classes.getClasses(), 0.9);

      if (newClass == null) {
        throw TextException.of("match.class.notFound");
      }

      try {
        classes.setPlayerClass(player.getId(), newClass);
      } catch (IllegalStateException e) {
        throw TextException.of("match.class.sticky");
      }

      player.sendMessage(
          Component.translatable(
              "match.class.ok",
              NamedTextColor.GREEN,
              text(newClass.getName(), NamedTextColor.GOLD, TextDecoration.UNDERLINED)));
      if (player.isParticipating()) {
        player.sendMessage(Component.translatable("match.class.queue", NamedTextColor.GREEN));
      }
    }
  }

  @Command(
      aliases = {"classlist", "classes", "listclasses", "cls"},
      desc = "List all available classes")
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
      StringBuilder result = new StringBuilder();

      result.append(i++).append(". ");

      if (cls == currentClass) {
        result.append(ChatColor.GOLD);
      } else if (cls.canUse(player.getBukkit())) {
        result.append(ChatColor.GREEN);
      } else {
        result.append(ChatColor.RED);
      }

      if (cls == currentClass) result.append(ChatColor.UNDERLINE);
      result.append(cls.getName());

      if (cls.getDescription() != null) {
        result
            .append(ChatColor.DARK_PURPLE)
            .append(" - ")
            .append(ChatColor.RESET)
            .append(cls.getDescription());
      }

      player.sendMessage(text(result.toString()));
    }
  }

  private ClassMatchModule getClasses(Match match) {
    final ClassMatchModule classes = match.getModule(ClassMatchModule.class);
    if (classes == null) {
      throw TextException.of("match.class.notEnabled");
    }
    return classes;
  }
}

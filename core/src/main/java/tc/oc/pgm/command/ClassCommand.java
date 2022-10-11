package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Text;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.command.graph.Sender;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.text.TextTranslations;

public final class ClassCommand {

  @Command(
      aliases = {"class", "selectclass", "c", "cl"},
      desc = "Select your class")
  public void classSelect(Sender.Player sender, @Nullable @Text String query) {
    final ClassMatchModule classes = getClasses(sender.getMatch());
    final PlayerClass currentClass = classes.getSelectedClass(sender.getId());

    if (query == null) {
      sender.sendMessage(
          translatable("match.class.current", NamedTextColor.GREEN)
              .append(space())
              .append(text(currentClass.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
      sender.sendMessage(translatable("match.class.view", NamedTextColor.GOLD));
    } else {
      final PlayerClass newClass = StringUtils.bestFuzzyMatch(query, classes.getClasses(), 0.9);

      if (newClass == null) {
        throw exception("match.class.notFound");
      }

      try {
        classes.setPlayerClass(sender.getPlayer().getId(), newClass);
      } catch (IllegalStateException e) {
        throw exception("match.class.sticky");
      }

      sender.sendMessage(
          translatable(
              "match.class.ok",
              NamedTextColor.GREEN,
              text(newClass.getName(), NamedTextColor.GOLD, TextDecoration.UNDERLINED)));
      if (sender.getPlayer().isParticipating()) {
        sender.sendMessage(translatable("match.class.queue", NamedTextColor.GREEN));
      }
    }
  }

  @Command(
      aliases = {"classlist", "classes", "listclasses", "cls"},
      desc = "List all available classes")
  public void classList(Sender.Player player) {
    final ClassMatchModule classes = getClasses(player.getMatch());
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    player.sendMessage(
        text(
            LegacyFormatUtils.dashedChatMessage(
                ChatColor.GOLD
                    + TextTranslations.translate("match.class.title", player.getSender()),
                "-",
                ChatColor.RED.toString())));
    int i = 1;
    for (PlayerClass cls : classes.getClasses()) {
      TextComponent.Builder result = text().append(text(i++ + ". "));

      NamedTextColor color;

      if (cls == currentClass) {
        color = NamedTextColor.GOLD;
      } else if (cls.canUse(player.getSender())) {
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
      throw exception("match.class.notEnabled");
    }
    return classes;
  }
}

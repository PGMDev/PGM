package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.util.text.TextFormatter;

public final class ClassCommand {

  @Command("class|selectclass|c|cl [class]")
  @CommandDescription("Select your class")
  public void classSelect(
      ClassMatchModule classes,
      MatchPlayer player,
      @Argument("class") @Greedy PlayerClass newClass) {
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    if (newClass == null) {
      player.sendMessage(translatable("match.class.current", NamedTextColor.GREEN)
          .append(space())
          .append(currentClass.getComponent()));
      player.sendMessage(translatable("match.class.view", NamedTextColor.GOLD));
    } else {
      classes.setPlayerClass(player.getId(), newClass);

      player.sendMessage(
          translatable("match.class.ok", NamedTextColor.GREEN, newClass.getComponent()));
      if (player.isParticipating()) {
        player.sendMessage(translatable("match.class.queue", NamedTextColor.GREEN));
      }
    }
  }

  @Command("classlist|classes|listclasses|cls")
  @CommandDescription("List all available classes")
  public void classList(ClassMatchModule classes, MatchPlayer player) {
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    player.sendMessage(TextFormatter.horizontalLineHeading(
        player.getBukkit(),
        translatable("match.class.title").color(NamedTextColor.GOLD),
        NamedTextColor.RED));

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

      result.append(text(cls.getName(), color)
          .decoration(TextDecoration.UNDERLINED, cls == currentClass)
          .hoverEvent(showText(translatable(
              "match.class.select", NamedTextColor.GRAY, text(cls.getName(), NamedTextColor.GOLD))))
          .clickEvent(runCommand("/class " + cls.getName())));

      if (cls.getDescription() != null) {
        result.append(text(" - ", NamedTextColor.DARK_PURPLE)).append(text(cls.getDescription()));
      }

      player.sendMessage(result.build());
    }
  }
}

package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.specifier.Greedy;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.util.text.TextFormatter;

public final class ClassCommand {

  @CommandMethod("class|selectclass|c|cl [class]")
  @CommandDescription("Select your class")
  public void classSelect(
      ClassMatchModule classes,
      MatchPlayer player,
      @Argument("class") @Greedy PlayerClass newClass) {
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    if (newClass == null) {
      player.sendMessage(
          translatable("match.class.current", NamedTextColor.GREEN)
              .append(space())
              .append(text(currentClass.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
      player.sendMessage(translatable("match.class.view", NamedTextColor.GOLD));
    } else {
      classes.setPlayerClass(player.getId(), newClass);

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

  @CommandMethod("classlist|classes|listclasses|cls")
  @CommandDescription("List all available classes")
  public void classList(ClassMatchModule classes, MatchPlayer player) {
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    player.sendMessage(
        TextFormatter.horizontalLineHeading(
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

      result.append(
          text(cls.getName(), color).decoration(TextDecoration.UNDERLINED, cls == currentClass));

      if (cls.getDescription() != null) {
        result.append(text(" - ", NamedTextColor.DARK_PURPLE)).append(text(cls.getDescription()));
      }

      player.sendMessage(result.build());
    }
  }
}

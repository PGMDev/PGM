package tc.oc.pgm.command;

public final class ClassCommand {
  /*
  @Command(
      aliases = {"class", "selectclass", "c", "cl"},
      desc = "Select your class")
  public void classSelect(Match match, MatchPlayer player, @Maybe @Text String query) {
    final ClassMatchModule classes = getClasses(match);
    final PlayerClass currentClass = classes.getSelectedClass(player.getId());

    if (query == null) {
      player.sendMessage(
          translatable("match.class.current", NamedTextColor.GREEN)
              .append(space())
              .append(text(currentClass.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
      player.sendMessage(translatable("match.class.view", NamedTextColor.GOLD));
    } else {
      final PlayerClass newClass = StringUtils.bestFuzzyMatch(query, classes.getClasses());

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
      throw exception("match.class.notEnabled");
    }
    return classes;
  }*/
}

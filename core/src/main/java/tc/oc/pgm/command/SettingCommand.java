package tc.oc.pgm.command;

// TODO: remove some of these when settings UI is released
public final class SettingCommand {
  /*
  @Command(
      aliases = {"settings"},
      desc = "Open the settings menu")
  public void settings(MatchPlayer player) {
    new SettingsMenu(player);
  }

  @Command(
      aliases = {"tools", "observertools", "ot"},
      desc = "Open the observer tools menu")
  public void observerTools(MatchPlayer player) {
    if (player.isObserving()) {
      final ObserverToolsMatchModule tools =
          player.getMatch().getModule(ObserverToolsMatchModule.class);
      if (tools != null) {
        tools.openMenu(player);
      }
    } else {
      // TODO: reconsider when observer tools become settings
      throw exception("setting.observersOnly");
    }
  }

  @Command(
      aliases = {"setting"},
      desc = "Get the value of a setting",
      usage = "[setting name]")
  public void setting(MatchPlayer player, SettingKey key) {
    final SettingValue value = player.getSettings().getValue(key);

    sendCurrentSetting(player, key, value);
    player.sendMessage(
        translatable(
            "setting.options",
            TextFormatter.list(
                Stream.of(key.getPossibleValues())
                    .map(option -> text(option.getName(), NamedTextColor.GRAY))
                    .collect(Collectors.toList()),
                NamedTextColor.WHITE)));
  }

  @Command(
      aliases = {"toggle", "set"},
      desc = "Toggle or set the value of a setting",
      usage = "[setting name] <option>")
  public void toggle(
      CommandSender sender, MatchPlayer player, SettingKey key, @Text @Maybe String query) {
    final Settings setting = player.getSettings();
    final SettingValue old = setting.getValue(key);

    final SettingValue value;
    if (query == null) {
      setting.toggleValue(key);
      value = setting.getValue(key);
    } else {
      value = SettingValue.search(key, query);
      setting.setValue(key, value);
    }

    if (old == value) {
      sendCurrentSetting(player, key, old);
    } else {
      player.sendMessage(
          translatable(
              "setting.set",
              text(key.getName()),
              text(old.getName(), NamedTextColor.GRAY),
              text(value.getName(), NamedTextColor.GREEN)));
      key.update(player);
    }
  }

  private void sendCurrentSetting(MatchPlayer player, SettingKey key, SettingValue value) {
    player.sendMessage(
        translatable(
            "setting.get", text(key.getName()), text(value.getName(), NamedTextColor.GREEN)));
  }*/
}

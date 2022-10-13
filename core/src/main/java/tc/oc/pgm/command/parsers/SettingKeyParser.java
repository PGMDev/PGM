package tc.oc.pgm.command.parsers;

/*
final class SettingKeyParser implements ParserSupplier<SettingKey> {

  @Override
  public String getName() {
    return "setting";
  }

  @Override
  public SettingKey get(
      CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException {
    final String query = args.next().toLowerCase();

    for (SettingKey settingKey : SettingKey.values()) {
      if (settingKey.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(query))) {
        return settingKey;
      }
    }

    throw invalidFormat(query, SettingKey.class, null);
  }

  @Override
  public List<String> getSuggestions(
      String prefix, Namespace namespace, List<? extends Annotation> modifiers) {
    final String query = prefix.toLowerCase();
    final List<String> suggestions = new ArrayList<>();

    for (SettingKey settingKey : SettingKey.values()) {
      for (String alias : settingKey.getAliases()) {
        if (alias.toLowerCase().startsWith(query.toLowerCase())) {
          suggestions.add(alias);
          break; // don't suggest more aliases for this setting
        }
      }
    }

    return suggestions;
  }
}
*/

package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.ArgumentParseException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.Namespace;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.setting.SettingKey;

public class SettingKeyProvider implements BukkitProvider<SettingKey> {

  @Override
  public String getName() {
    return "setting key";
  }

  @Nullable
  @Override
  public SettingKey get(
      CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException, ProvisionException {
    String query = args.next().toLowerCase();
    for (SettingKey settingKey : SettingKey.values()) {
      if (settingKey.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(query))) {
        return settingKey;
      }
    }

    throw new ArgumentParseException("No matching setting found.");
  }

  @Override
  public List<String> getSuggestions(
      String prefix, Namespace namespace, List<? extends Annotation> modifiers) {
    String query = prefix.toLowerCase();

    List<String> suggestions = new ArrayList<>();
    for (SettingKey settingKey : SettingKey.values()) {
      for (String alias : settingKey.getAliases()) {
        if (alias.toLowerCase().startsWith(query.toLowerCase())) {
          suggestions.add(alias);
        }
      }
    }

    return suggestions;
  }
}

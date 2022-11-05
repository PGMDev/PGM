package tc.oc.pgm.command.parsers;

import cloud.commandframework.context.CommandContext;
import java.util.Arrays;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.command.util.CommandKeys;

/**
 * An extended enum parser that will limit the available options based on a previous setting key
 * parameter.
 */
public final class SettingValueParser extends EnumParser<SettingValue> {

  public SettingValueParser() {
    super(SettingValue.class);
  }

  @Override
  protected Stream<SettingValue> options(CommandContext<CommandSender> context) {
    SettingKey key = context.get(CommandKeys.SETTING_KEY);
    return Arrays.stream(key.getPossibleValues());
  }

  @Override
  protected String stringify(SettingValue val) {
    return val.getName();
  }
}

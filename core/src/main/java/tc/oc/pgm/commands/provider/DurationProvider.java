package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.text.TextParser;

public class DurationProvider implements BukkitProvider<Duration> {

  @Override
  public String getName() {
    return "duration";
  }

  @Override
  public Duration get(
      CommandSender commandSender, CommandArgs commandArgs, List<? extends Annotation> list)
      throws ArgumentException, ProvisionException {
    if (commandArgs.hasNext()) {
      return TextParser.parseDuration(commandArgs.next());
    }
    return null;
  }
}

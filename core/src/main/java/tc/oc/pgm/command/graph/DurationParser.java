package tc.oc.pgm.command.graph;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.text.TextParser;

final class DurationParser implements BukkitProvider<Duration> {

  @Override
  public String getName() {
    return "duration";
  }

  @Override
  public Duration get(
      CommandSender commandSender, CommandArgs commandArgs, List<? extends Annotation> list)
      throws MissingArgumentException {
    if (commandArgs.hasNext()) {
      return TextParser.parseDuration(commandArgs.next());
    }
    return Duration.ZERO;
  }
}

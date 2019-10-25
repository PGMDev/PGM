package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

public class VectorProvider implements BukkitProvider<Vector> {

  @Override
  public String getName() {
    return "vector";
  }

  @Override
  public Vector get(
      CommandSender commandSender, CommandArgs commandArgs, List<? extends Annotation> list)
      throws ArgumentException, ProvisionException {
    return new Vector(commandArgs.nextDouble(), commandArgs.nextDouble(), commandArgs.nextDouble());
  }
}

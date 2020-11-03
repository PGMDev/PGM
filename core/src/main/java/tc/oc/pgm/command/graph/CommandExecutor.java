package tc.oc.pgm.command.graph;

import static tc.oc.pgm.PGMAudiences.sendWarning;

import app.ashcon.intake.CommandException;
import app.ashcon.intake.InvalidUsageException;
import app.ashcon.intake.InvocationCommandException;
import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.fluent.CommandGraph;
import app.ashcon.intake.util.auth.AuthorizationException;
import com.google.common.base.Joiner;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.text.TextException;

public final class CommandExecutor extends BukkitIntake {

  public CommandExecutor(Plugin plugin, CommandGraph commandGraph) {
    super(plugin, commandGraph);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    final Audience audience = PGM.get().getPGMAudiences().PROVIDER.sender(sender);

    try {
      return this.getCommandGraph()
          .getRootDispatcherNode()
          .getDispatcher()
          .call(this.getCommand(command, args), this.getNamespace(sender));
    } catch (AuthorizationException e) {
      sendWarning(Component.translatable("misc.noPermission"), audience);
    } catch (InvocationCommandException e) {
      if (e.getCause() instanceof TextException) {
        sendWarning(((TextException) e.getCause()).getText(), audience);
      } else {
        sendWarning(TextException.unknown(e).getText(), audience);
        e.printStackTrace();
      }
    } catch (InvalidUsageException e) {
      if (e.getMessage() != null) {
        sendWarning(Component.text(e.getMessage()), audience);
      }

      if (e.isFullHelpSuggested()) {
        sendWarning(
            Component.text(
                "/"
                    + Joiner.on(' ').join(e.getAliasStack())
                    + " "
                    + e.getCommand().getDescription().getUsage()),
            audience);
      }
    } catch (CommandException e) {
      audience.sendMessage(Component.text(e.getMessage()));
    }

    return false;
  }
}

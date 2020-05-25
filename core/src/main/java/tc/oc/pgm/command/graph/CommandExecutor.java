package tc.oc.pgm.command.graph;

import app.ashcon.intake.CommandException;
import app.ashcon.intake.InvalidUsageException;
import app.ashcon.intake.InvocationCommandException;
import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.fluent.CommandGraph;
import app.ashcon.intake.util.auth.AuthorizationException;
import com.google.common.base.Joiner;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.TextException;

public final class CommandExecutor extends BukkitIntake {

  public CommandExecutor(Plugin plugin, CommandGraph commandGraph) {
    super(plugin, commandGraph);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    final Audience audience = Audience.get(sender);

    try {
      return this.getCommandGraph()
          .getRootDispatcherNode()
          .getDispatcher()
          .call(this.getCommand(command, args), this.getNamespace(sender));
    } catch (AuthorizationException e) {
      audience.sendWarning(TranslatableComponent.of("misc.noPermission"));
    } catch (InvocationCommandException e) {
      if (e.getCause() instanceof TextException) {
        audience.sendWarning(((TextException) e.getCause()).getText());
      } else {
        audience.sendWarning(TextException.unknown(e).getText());
        e.printStackTrace();
      }
    } catch (InvalidUsageException e) {
      if (e.getMessage() != null) {
        audience.sendWarning(TextComponent.of(e.getMessage()));
      }

      if (e.isFullHelpSuggested()) {
        audience.sendWarning(
            TextComponent.of(
                "/"
                    + Joiner.on(' ').join(e.getAliasStack())
                    + " "
                    + e.getCommand().getDescription().getUsage()));
      }
    } catch (CommandException e) {
      audience.sendMessage(TextComponent.of(e.getMessage()));
    }

    return false;
  }
}

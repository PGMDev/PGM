package tc.oc.pgm.command.util;

/*
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
      audience.sendWarning(translatable("misc.noPermission"));
    } catch (InvocationCommandException e) {
      if (e.getCause() instanceof ComponentMessageThrowable) {
        final Component message = ((ComponentMessageThrowable) e.getCause()).componentMessage();
        if (message != null) {
          audience.sendWarning(message);
        }
      } else {
        audience.sendWarning(unknown(e).componentMessage());
        e.printStackTrace();
      }
    } catch (InvalidUsageException e) {
      if (e.getMessage() != null) {
        audience.sendWarning(text(e.getMessage()));
      }

      if (e.isFullHelpSuggested()) {
        audience.sendWarning(
            text(
                "/"
                    + Joiner.on(' ').join(e.getAliasStack())
                    + " "
                    + e.getCommand().getDescription().getUsage()));
      }
    } catch (CommandException e) {
      audience.sendMessage(text(e.getMessage()));
    }

    return false;
  }
}
*/

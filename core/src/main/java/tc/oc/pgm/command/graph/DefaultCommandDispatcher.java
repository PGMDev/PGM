package tc.oc.pgm.command.graph;

import app.ashcon.intake.CommandException;
import app.ashcon.intake.CommandMapping;
import app.ashcon.intake.InvalidUsageException;
import app.ashcon.intake.InvocationCommandException;
import app.ashcon.intake.argument.CommandContext;
import app.ashcon.intake.argument.Namespace;
import app.ashcon.intake.dispatcher.SimpleDispatcher;
import app.ashcon.intake.parametric.ProvisionException;
import app.ashcon.intake.util.auth.AuthorizationException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DefaultCommandDispatcher extends SimpleDispatcher {

  protected final String defaultCommand;

  public DefaultCommandDispatcher(String defaultCommand) {
    this.defaultCommand = defaultCommand;
  }

  @Override
  public boolean call(String arguments, Namespace namespace, List<String> parentCommands)
      throws CommandException, InvocationCommandException, AuthorizationException {
    if (!testPermission(namespace)) {
      throw new AuthorizationException();
    }

    String[] split = CommandContext.split(arguments);
    Set<String> aliases = getPrimaryAliases();

    if (aliases.isEmpty()) {
      throw new ProvisionException("There are no sub-commands for " + parentCommands);
    } else if (arguments.length() == 0) {
      CommandMapping defaultMapping = get(defaultCommand);
      List<String> defaultSubParents =
          ImmutableList.<String>builder().addAll(parentCommands).add(defaultCommand).build();
      if (defaultMapping != null) {
        try {
          defaultMapping.getCallable().call("", namespace, defaultSubParents);
        } catch (AuthorizationException e) {
          throw e;
        } catch (CommandException e) {
          throw e;
        } catch (InvocationCommandException e) {
          throw e;
        } catch (Throwable t) {
          throw new InvocationCommandException(t);
        }

        return true;
      }
    } else if (split.length > 0) {
      String subCommand = split[0];
      String subArguments = Joiner.on(" ").join(Arrays.copyOfRange(split, 1, split.length));
      List<String> subParents =
          ImmutableList.<String>builder().addAll(parentCommands).add(subCommand).build();
      CommandMapping mapping = get(subCommand);

      if (mapping != null) {
        try {
          mapping.getCallable().call(subArguments, namespace, subParents);
        } catch (AuthorizationException e) {
          throw e;
        } catch (CommandException e) {
          throw e;
        } catch (InvocationCommandException e) {
          throw e;
        } catch (Throwable t) {
          throw new InvocationCommandException(t);
        }

        return true;
      }
    }

    throw new InvalidUsageException(null, this, parentCommands, true);
  }
}

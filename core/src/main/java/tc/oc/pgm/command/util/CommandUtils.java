package tc.oc.pgm.command.util;

import cloud.commandframework.context.CommandContext;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class CommandUtils {

  public static Match getMatch(CommandContext<CommandSender> context) {
    return context.computeIfAbsent(
        CommandKeys.MATCH, k -> PGM.get().getMatchManager().getMatch(context.getSender()));
  }
}

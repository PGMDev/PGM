package tc.oc.pgm.command.util;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserParameters;

public interface ParserBuilder<T> {

  ArgumentParser<CommandSender, T> create(
      CommandManager<CommandSender> manager, ParserParameters options);
}

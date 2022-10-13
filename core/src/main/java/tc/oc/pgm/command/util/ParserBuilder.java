package tc.oc.pgm.command.util;

import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;

public interface ParserBuilder<T> {

  ArgumentParser<CommandSender, T> create(
      PaperCommandManager<CommandSender> manager, ParserParameters options);
}

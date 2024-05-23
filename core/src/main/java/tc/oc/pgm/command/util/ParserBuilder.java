package tc.oc.pgm.command.util;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserParameters;

public interface ParserBuilder<T> {

  ArgumentParser<CommandSender, T> create(
      LegacyPaperCommandManager<CommandSender> manager, ParserParameters options);
}

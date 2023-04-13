package tc.oc.pgm.command.parsers;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collection;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.util.text.TextException;

public final class PlayerClassParser
    extends MatchObjectParser.Simple<PlayerClass, ClassMatchModule> {

  public PlayerClassParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, PlayerClass.class, ClassMatchModule.class, "classes");
  }

  @Override
  protected Collection<PlayerClass> objects(ClassMatchModule module) {
    return module.getClasses();
  }

  @Override
  protected String getName(PlayerClass obj) {
    return obj.getName();
  }

  @Override
  protected TextException moduleNotFound() {
    return exception("match.class.notEnabled");
  }
}

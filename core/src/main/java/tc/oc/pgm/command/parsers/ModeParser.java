package tc.oc.pgm.command.parsers;

import java.util.Collection;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.ParserParameters;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;

public class ModeParser extends MatchObjectParser.Simple<Mode, ObjectiveModesMatchModule> {

  public ModeParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Mode.class, ObjectiveModesMatchModule.class, "modes");
  }

  @Override
  protected Collection<Mode> objects(ObjectiveModesMatchModule module) {
    return module.getModes();
  }

  @Override
  protected String getName(Mode mode) {
    return mode.getId();
  }
}

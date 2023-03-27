package tc.oc.pgm.command.parsers;

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collection;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.action.ActionMatchModule;
import tc.oc.pgm.action.actions.ExposedAction;

public class ExposedActionParser
    extends MatchObjectParser.Simple<ExposedAction, ActionMatchModule> {

  public ExposedActionParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, ExposedAction.class, ActionMatchModule.class, "actions");
  }

  @Override
  protected Collection<ExposedAction> objects(ActionMatchModule module) {
    return module.getExposedActions();
  }

  @Override
  protected String getName(ExposedAction obj) {
    return obj.getId();
  }
}

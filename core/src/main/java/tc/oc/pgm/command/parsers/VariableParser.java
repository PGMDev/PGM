package tc.oc.pgm.command.parsers;

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

@SuppressWarnings("rawtypes")
public class VariableParser extends MatchObjectParser.Simple<Variable, VariablesMatchModule> {

  public VariableParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Variable.class, VariablesMatchModule.class, "variables");
  }

  @Override
  protected Iterable<Variable> objects(VariablesMatchModule module) {
    return module.getVariables();
  }

  @Override
  protected String getName(Variable variable) {
    return variable.getId();
  }
}

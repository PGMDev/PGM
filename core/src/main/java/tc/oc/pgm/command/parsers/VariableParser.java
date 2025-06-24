package tc.oc.pgm.command.parsers;

import java.util.Map;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.ParserParameters;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

@SuppressWarnings("rawtypes")
public class VariableParser
    extends MatchObjectParser<Variable, Map.Entry<String, Variable<?>>, VariablesMatchModule> {

  public VariableParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Variable.class, VariablesMatchModule.class, "variables");
  }

  @Override
  protected Iterable<Map.Entry<String, Variable<?>>> objects(VariablesMatchModule module) {
    return () -> module.getVariables().iterator();
  }

  @Override
  protected String getName(Map.Entry<String, Variable<?>> obj) {
    return obj.getKey();
  }

  @Override
  protected Variable getValue(Map.Entry<String, Variable<?>> obj) {
    return obj.getValue();
  }
}

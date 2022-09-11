package tc.oc.pgm.action;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.variables.VariablesModule;

public class ActionModule implements MapModule<ActionMatchModule> {

  private final ImmutableList<Trigger<?>> triggers;

  public ActionModule(ImmutableList<Trigger<?>> triggers) {
    this.triggers = triggers;
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Nullable
  @Override
  public ActionMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new ActionMatchModule(match, triggers);
  }

  public static class Factory implements MapModuleFactory<ActionModule> {

    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(VariablesModule.class);
    }

    @Nullable
    @Override
    public ActionModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      ActionParser parser = new ActionParser(factory);

      for (Element actions : doc.getRootElement().getChildren("actions")) {
        for (Element action : actions.getChildren()) {
          if (parser.isAction(action)) parser.parse(action, null);
        }
      }

      ImmutableList.Builder<Trigger<?>> triggers = ImmutableList.builder();
      for (Element actions : doc.getRootElement().getChildren("actions")) {
        for (Element rule : actions.getChildren("trigger")) {
          triggers.add(parser.parseTrigger(rule));
        }
      }

      return new ActionModule(triggers.build());
    }
  }
}

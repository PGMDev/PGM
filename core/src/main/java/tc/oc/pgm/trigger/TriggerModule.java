package tc.oc.pgm.trigger;

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
import tc.oc.pgm.filters.dynamic.FilterMatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class TriggerModule implements MapModule {

  private final ImmutableList<TriggerRule<?>> triggerRules;

  public TriggerModule(ImmutableList<TriggerRule<?>> triggerRules) {
    this.triggerRules = triggerRules;
  }

  @Nullable
  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(FilterMatchModule.class);
  }

  @Nullable
  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new TriggerMatchModule(match, triggerRules);
  }

  public static class Factory implements MapModuleFactory<TriggerModule> {

    @Nullable
    @Override
    public TriggerModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      TriggerParser parser = new TriggerParser(factory);

      for (Element triggers : doc.getRootElement().getChildren("triggers")) {
        for (Element trigger : triggers.getChildren()) {
          if (parser.isTrigger(trigger)) parser.parse(trigger, null);
        }
      }

      ImmutableList.Builder<TriggerRule<?>> triggerRules = ImmutableList.builder();
      for (Element triggers : doc.getRootElement().getChildren("triggers")) {
        for (Element rule : triggers.getChildren("when")) {
          triggerRules.add(parser.parseRule(rule));
        }
      }

      return new TriggerModule(triggerRules.build());
    }
  }
}

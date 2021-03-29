package tc.oc.pgm.timeadjust;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.core.CoreMatchModule;
import tc.oc.pgm.core.CoreModule;
import tc.oc.pgm.destroyable.DestroyableMatchModule;
import tc.oc.pgm.destroyable.DestroyableModule;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.wool.WoolMatchModule;
import tc.oc.pgm.wool.WoolModule;

public class TimeAdjustModule implements MapModule<TimeAdjustMatchModule> {

  private final Map<GoalDefinition, TimeAdjust> timeAdjusts;

  public TimeAdjustModule(Map<GoalDefinition, TimeAdjust> timeAdjusts) {
    this.timeAdjusts = timeAdjusts;
  }

  @Override
  public TimeAdjustMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new TimeAdjustMatchModule(match, timeAdjusts);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getWeakDependencies() {
    return ImmutableList.of(
        DestroyableMatchModule.class, WoolMatchModule.class, CoreMatchModule.class);
  }

  public static class Factory implements MapModuleFactory<TimeAdjustModule> {

    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(DestroyableModule.class, WoolModule.class, CoreModule.class);
    }

    @Override
    public TimeAdjustModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<GoalDefinition, TimeAdjust> adjustments = Maps.newHashMap();

      for (Element adjustEl :
          XMLUtils.flattenElements(doc.getRootElement(), "time-adjust", "adjust")) {

        String goalId = adjustEl.getAttributeValue("id");

        FeatureDefinition goal = factory.getFeatures().get(goalId);
        if (goal == null) {
          throw new InvalidXMLException(
              "No objective with the id (" + goalId + ") was found!", adjustEl);
        }

        if (!(goal instanceof GoalDefinition)) {
          throw new InvalidXMLException(goalId + " is not a valid objective id", adjustEl);
        }
        TimeAdjust adjust = parseTimeAdjust(factory, adjustEl);
        adjustments.put((GoalDefinition) goal, adjust);
      }
      return new TimeAdjustModule(adjustments);
    }

    private static TimeAdjust parseTimeAdjust(MapFactory factory, Element el)
        throws InvalidXMLException {
      return new TimeAdjust(
          XMLUtils.parseDuration(el.getAttribute("time")),
          XMLUtils.parseBoolean(el.getAttribute("broadcast"), true));
    }
  }
}

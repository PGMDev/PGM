package tc.oc.pgm.controlpoint;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class ControlPointModule implements MapModule {

  private final List<ControlPointDefinition> definitions;

  public ControlPointModule(List<ControlPointDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    List<ControlPoint> controlPoints = new LinkedList<>();

    for (ControlPointDefinition definition : this.definitions) {
      ControlPoint controlPoint = new ControlPoint(match, definition);
      match.getFeatureContext().add(controlPoint);
      match.needModule(GoalMatchModule.class).addGoal(controlPoint);
      controlPoints.add(controlPoint);
    }

    return new ControlPointMatchModule(match, controlPoints);
  }

  public static class Factory implements MapModuleFactory<ControlPointModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(TeamModule.class, RegionModule.class, FilterModule.class);
    }

    @Override
    public ControlPointModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<ControlPointDefinition> definitions = new ArrayList<>();

      for (Element elControlPoint :
          XMLUtils.flattenElements(doc.getRootElement(), "control-points", "control-point")) {
        ControlPointDefinition definition =
            ControlPointParser.parseControlPoint(factory, elControlPoint, false);
        factory.getFeatures().addFeature(elControlPoint, definition);
        definitions.add(definition);
      }

      for (Element kingEl : doc.getRootElement().getChildren("king")) {
        for (Element hillEl : XMLUtils.flattenElements(kingEl, "hills", "hill")) {
          ControlPointDefinition definition =
              ControlPointParser.parseControlPoint(factory, hillEl, true);
          factory.getFeatures().addFeature(kingEl, definition);
          definitions.add(definition);
        }
      }

      if (!definitions.isEmpty()) {
        return new ControlPointModule(definitions);
      } else {
        return null;
      }
    }
  }
}

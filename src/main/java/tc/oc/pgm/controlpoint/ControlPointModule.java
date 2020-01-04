package tc.oc.pgm.controlpoint;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.GoalModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.maptag.MapTagSet;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Control Points",
    depends = {TeamModule.class, GoalModule.class, RegionModule.class, FilterModule.class})
public class ControlPointModule extends MapModule {

  private static final MapTag CONTROLPOINT_TAG = MapTag.forName("controlpoint");

  private final List<ControlPointDefinition> definitions;

  public ControlPointModule(List<ControlPointDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public void loadTags(MapTagSet tags) {
    tags.add(CONTROLPOINT_TAG);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    ImmutableList.Builder<ControlPoint> controlPoints = new ImmutableList.Builder<>();

    for (ControlPointDefinition definition : this.definitions) {
      ControlPoint controlPoint = new ControlPoint(match, definition);
      match.getFeatureContext().add(controlPoint);
      match.needMatchModule(GoalMatchModule.class).addGoal(controlPoint);
      controlPoints.add(controlPoint);
    }

    return new ControlPointMatchModule(match, controlPoints.build());
  }

  public static ControlPointModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<ControlPointDefinition> definitions = new ArrayList<>();

    for (Element elControlPoint :
        XMLUtils.flattenElements(doc.getRootElement(), "control-points", "control-point")) {
      ControlPointDefinition definition =
          ControlPointParser.parseControlPoint(context, elControlPoint, false);
      context.features().addFeature(elControlPoint, definition);
      definitions.add(definition);
    }

    for (Element kingEl : doc.getRootElement().getChildren("king")) {
      for (Element hillEl : XMLUtils.flattenElements(kingEl, "hills", "hill")) {
        ControlPointDefinition definition =
            ControlPointParser.parseControlPoint(context, hillEl, true);
        context.features().addFeature(kingEl, definition);
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

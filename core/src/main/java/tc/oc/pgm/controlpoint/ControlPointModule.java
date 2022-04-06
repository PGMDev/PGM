package tc.oc.pgm.controlpoint;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.XMLUtils;

public class ControlPointModule implements MapModule<ControlPointMatchModule> {

  private static final Collection<MapTag> TAGS_CP =
      ImmutableList.of(new MapTag("cp", "controlpoint", "Control the Point", true, false));
  private static final Collection<MapTag> TAGS_KOTH =
      ImmutableList.of(new MapTag("koth", "controlpoint", "King of the Hill", true, false));
  private final List<ControlPointDefinition> definitions;
  private final boolean koth;

  public ControlPointModule(List<ControlPointDefinition> definitions, boolean koth) {
    this.definitions = definitions;
    this.koth = koth;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public ControlPointMatchModule createMatchModule(Match match) {
    List<ControlPoint> controlPoints = new LinkedList<>();

    for (ControlPointDefinition definition : this.definitions) {
      ControlPoint controlPoint = new ControlPoint(match, definition);
      match.getFeatureContext().add(controlPoint);
      match.needModule(GoalMatchModule.class).addGoal(controlPoint);
      controlPoints.add(controlPoint);
    }

    return new ControlPointMatchModule(match, controlPoints);
  }

  @Override
  public Collection<MapTag> getTags() {
    return this.koth ? TAGS_KOTH : TAGS_CP;
  }

  public static class Factory implements MapModuleFactory<ControlPointModule> {
    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public ControlPointModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<ControlPointDefinition> definitions = new ArrayList<>();
      AtomicInteger serialNumber = new AtomicInteger(1);
      boolean koth = false;

      for (Element elControlPoint :
          XMLUtils.flattenElements(doc.getRootElement(), "control-points", "control-point")) {
        ControlPointDefinition definition =
            ControlPointParser.parseControlPoint(factory, elControlPoint, false, serialNumber);
        factory.getFeatures().addFeature(elControlPoint, definition);
        definitions.add(definition);
      }

      for (Element kingEl : doc.getRootElement().getChildren("king")) {
        for (Element hillEl : XMLUtils.flattenElements(kingEl, "hills", "hill")) {
          koth = true;
          ControlPointDefinition definition =
              ControlPointParser.parseControlPoint(factory, hillEl, true, serialNumber);
          factory.getFeatures().addFeature(kingEl, definition);
          definitions.add(definition);
        }
      }

      if (!definitions.isEmpty()) {
        return new ControlPointModule(definitions, koth);
      } else {
        return null;
      }
    }
  }
}

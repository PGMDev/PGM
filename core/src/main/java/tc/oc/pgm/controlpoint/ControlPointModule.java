package tc.oc.pgm.controlpoint;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.controlpoint.ControlPointParser.Type;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ControlPointModule implements MapModule<ControlPointMatchModule> {

  private static final MapTag CP =
      new MapTag("cp", "controlpoint", "Control the Point", true, false);
  private static final MapTag KOTH =
      new MapTag("koth", "controlpoint", "King of the Hill", true, false);
  private static final MapTag PAYLOAD = new MapTag("payload", "Payload", true, false);

  private final List<ControlPointDefinition> definitions;
  private final Collection<MapTag> tags;

  public ControlPointModule(List<ControlPointDefinition> definitions, Collection<MapTag> tags) {
    this.definitions = definitions;
    this.tags = tags;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public ControlPointMatchModule createMatchModule(Match match) {
    List<ControlPoint> controlPoints = new ArrayList<>(definitions.size());

    for (ControlPointDefinition definition : this.definitions) {
      ControlPoint controlPoint = definition.build(match);
      match.getFeatureContext().add(controlPoint);
      match.needModule(GoalMatchModule.class).addGoal(controlPoint);
      controlPoints.add(controlPoint);
    }

    return new ControlPointMatchModule(match, controlPoints);
  }

  @Override
  public Collection<MapTag> getTags() {
    return this.tags;
  }

  public static class Factory implements MapModuleFactory<ControlPointModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public ControlPointModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<ControlPointDefinition> definitions = new ArrayList<>();
      Set<MapTag> tags = new HashSet<>();
      AtomicInteger serialNumber = new AtomicInteger(1);

      for (Element elControlPoint :
          XMLUtils.flattenElements(doc.getRootElement(), "control-points", "control-point")) {
        tags.add(CP);
        ControlPointDefinition definition =
            ControlPointParser.parseControlPoint(factory, elControlPoint, Type.POINT, serialNumber);
        factory.getFeatures().addFeature(elControlPoint, definition);
        definitions.add(definition);
      }

      for (Element kingEl : doc.getRootElement().getChildren("king")) {
        for (Element hillEl : XMLUtils.flattenElements(kingEl, "hills", "hill")) {
          tags.add(KOTH);
          ControlPointDefinition definition =
              ControlPointParser.parseControlPoint(factory, hillEl, Type.HILL, serialNumber);
          factory.getFeatures().addFeature(kingEl, definition);
          definitions.add(definition);
        }
      }

      boolean payloadEnabled = PGM.get().getConfiguration().getExperimentAsBool("payload", false);

      for (Element payloadEl :
          XMLUtils.flattenElements(doc.getRootElement(), "payloads", "payload")) {
        if (!payloadEnabled)
          throw new InvalidXMLException(
              "Payload is in experimental & unfinished game-mode, everything bound to change, including XML syntax."
                  + "To enable the feature, change experiments.payload to true in pgm config.",
              payloadEl);

        tags.add(PAYLOAD);
        ControlPointDefinition definition =
            ControlPointParser.parseControlPoint(factory, payloadEl, Type.PAYLOAD, serialNumber);
        factory.getFeatures().addFeature(payloadEl, definition);
        definitions.add(definition);
      }

      if (!definitions.isEmpty()) {
        return new ControlPointModule(definitions, tags);
      } else {
        return null;
      }
    }
  }
}

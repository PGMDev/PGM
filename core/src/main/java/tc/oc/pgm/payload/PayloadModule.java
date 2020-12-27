package tc.oc.pgm.payload;

import com.google.common.collect.ImmutableList;
import java.util.*;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.goals.ControllableGoalDefinition;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class PayloadModule implements MapModule<PayloadMatchModule> {

  private final List<PayloadDefinition> definitions;

  public PayloadModule(List<PayloadDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class, TeamMatchModule.class);
  }

  @Nullable
  @Override
  public PayloadMatchModule createMatchModule(Match match) throws ModuleLoadException {

    final List<Payload> payloads = new LinkedList<>();

    for (PayloadDefinition definition : definitions) {
      Payload payload = new Payload(match, definition);
      match.getFeatureContext().add(payload);
      match.needModule(GoalMatchModule.class).addGoal(payload);
      payloads.add(payload);
    }

    return new PayloadMatchModule(match, payloads);
  }

  @Override
  public Collection<MapTag> getTags() {
    return ImmutableList.of(new MapTag("payload", "Payload", true, false));
  }

  public static class Factory implements MapModuleFactory<PayloadModule> {

    @Nullable
    @Override
    public PayloadModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<PayloadDefinition> definitions = new ArrayList<>();
      for (Element payloadEl :
          XMLUtils.flattenElements(doc.getRootElement(), "payloads", "payload")) {
        PayloadDefinition definition = parsePayloadDefinition(factory, payloadEl);
        factory.getFeatures().addFeature(payloadEl, definition);
        definitions.add(definition);
      }
      if (definitions.isEmpty()) return null;

      return new PayloadModule(definitions);
    }

    @Nullable
    @Override
    public Collection<Class<? extends MapModule>> getHardDependencies() {
      return ImmutableList.of(TeamModule.class);
    }
  }

  public static PayloadDefinition parsePayloadDefinition(MapFactory factory, Element el)
      throws InvalidXMLException {

    FilterParser filterParser = factory.getFilters();

    String id = el.getAttributeValue("id");
    String name = el.getAttributeValue("name", "Payload");
    boolean visible = XMLUtils.parseBoolean(el.getAttribute("visible"), true);

    Vector middleLocation = XMLUtils.parseVector(new Node(el.getChild("starting-location")));

    Filter playerPushFilter = filterParser.parseFilterProperty(el, "push-filter");
    Filter playerDominateFilter = filterParser.parseFilterProperty(el, "player-filter");

    TeamModule teams = factory.getModule(TeamModule.class);

    Map<TeamFactory, Vector> goalsByTeam = new HashMap<>();

    Element goalsEl = el.getChild("goals");

    if (goalsEl == null) throw new InvalidXMLException("The payload needs at least 1 goal", el);

    for (Element goalEl : goalsEl.getChildren("goal")) {
      goalsByTeam.put(
          teams.parseTeam(goalEl.getAttribute("team"), factory),
          XMLUtils.parseVector(new Node(goalEl)));
    }

    if (goalsByTeam.isEmpty())
      throw new InvalidXMLException("The Payload needs at least 1 team with a goal", goalsEl);
    if (goalsByTeam.size() != goalsByTeam.keySet().stream().distinct().count())
      throw new InvalidXMLException("A team can not have multiple goals", goalsEl);
    if (goalsByTeam.size() > 2)
      throw new InvalidXMLException("The Payload rail can not handle more than 2 goals", goalsEl);

    Map<TeamFactory, Float> speedsByTeam = new HashMap<>();

    Element speedsEl =
        el.getChild("speeds");

    float neutralSpeed = parseFloat("neutral-speed", 0.2f, speedsEl);
    if (neutralSpeed < 0)
      throw new InvalidXMLException("Neutral speed can not be under 0", speedsEl);

    for (Element speedEl : XMLUtils.getChildren(speedsEl, "speed")) {
      float speed = XMLUtils.parseNumber(new Node(speedEl), Float.class);
      if (speed < 0) throw new InvalidXMLException("Payload speed can not be under 0", speedEl);

      speedsByTeam.put(teams.parseTeam(speedEl.getAttribute("team"), factory), speed);
    }

    ControllableGoalDefinition.CaptureCondition captureCondition =
        ControllableGoalDefinition.parseCaptureCondition(el);

    Element propertiesEl = el.getChild("properties");

    float radius = parseFloat("radius", 3.5f, propertiesEl);
    float height = parseFloat("height", 5f, propertiesEl);

    if (radius < 0 || height < 0)
      throw new InvalidXMLException("Height and/or radius can not be under 0", propertiesEl);

    Element checkpointEl = el.getChild("checkpoints");

    List<PayloadCheckpoint> checkpoints = new ArrayList<>();

    if (checkpointEl != null) {
      for (Element checkpoint : checkpointEl.getChildren()) {
        checkpoints.add(
            new PayloadCheckpoint(
                checkpoint.getAttributeValue("id"),
                XMLUtils.parseVector(new Node(checkpoint)),
                XMLUtils.parseBoolean(checkpoint.getAttribute("permanent"), false)));
      }
    }

    boolean permanent = XMLUtils.parseBoolean(el.getAttribute("permanent"), false);

    float points = parseFloat("points", 0f, el);

    boolean showProgress = XMLUtils.parseBoolean(el.getAttribute("show-progress"), true);

    boolean required = XMLUtils.parseBoolean(el.getAttribute("required"), true);

    return new PayloadDefinition(
        id,
        name,
        required,
        visible,
        middleLocation,
        playerPushFilter,
        playerDominateFilter,
        goalsByTeam,
        speedsByTeam,
        captureCondition,
        radius,
        height,
        checkpoints,
        neutralSpeed,
        permanent,
        points,
        showProgress);
  }

  private static float parseFloat(String name, Float def, Element el) throws InvalidXMLException {
    if (el == null) return def;
    return XMLUtils.parseNumber(el.getAttribute(name), Float.class, def);
  }
}

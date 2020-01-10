package tc.oc.pgm.spawns;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.filters.AllFilter;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.filters.TeamFilter;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderAttributes;
import tc.oc.pgm.points.RandomPointProvider;
import tc.oc.pgm.points.SequentialPointProvider;
import tc.oc.pgm.points.SpreadPointProvider;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class SpawnParser {
  protected final MapContext context;
  protected final PointParser pointParser;
  protected @Nullable Spawn defaultSpawn;

  public SpawnParser(MapContext context, PointParser pointParser) {
    this.context = context;
    this.pointParser = pointParser;
  }

  public @Nullable Spawn getDefaultSpawn() {
    return defaultSpawn;
  }

  public Spawn parse(Element el, SpawnAttributes attributes) throws InvalidXMLException {
    attributes = this.parseAttributes(el, attributes);
    List<PointProvider> providers;

    if (context.getInfo().getProto().isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
      providers = this.pointParser.parse(el, attributes.providerAttributes);
    } else {
      providers =
          new ArrayList<>(
              pointParser.parseMultiProperty(el, attributes.providerAttributes, "region"));
      for (Element elRegions : XMLUtils.getChildren(el, "regions")) {
        providers.addAll(this.pointParser.parseChildren(elRegions, attributes.providerAttributes));
      }
    }

    PointProvider provider;
    if (attributes.sequential) {
      provider = new SequentialPointProvider(providers);
    } else if (attributes.spread) {
      provider = new SpreadPointProvider(providers);
    } else {
      provider = new RandomPointProvider(providers);
    }

    Spawn spawn = new Spawn(attributes, provider);
    context.legacy().getFeatures().addFeature(el, spawn);
    return spawn;
  }

  public List<Spawn> parseChildren(Element parent, SpawnAttributes attributes)
      throws InvalidXMLException {
    attributes = this.parseAttributes(parent, attributes);
    List<Spawn> spawns = Lists.newArrayList();
    for (Element spawnsEl : parent.getChildren("spawns")) {
      spawns.addAll(this.parseChildren(spawnsEl, attributes));
    }
    for (Element spawnEl : parent.getChildren("spawn")) {
      spawns.add(this.parse(spawnEl, attributes));
    }
    for (Element defaultEl : parent.getChildren("default")) {
      if (defaultSpawn != null) {
        throw new InvalidXMLException("Cannot have multiple default spawns", defaultEl);
      }
      this.defaultSpawn = parse(defaultEl, attributes);
    }
    return spawns;
  }

  public SpawnAttributes parseAttributes(Element el, SpawnAttributes parent)
      throws InvalidXMLException {
    PointProviderAttributes providerAttributes =
        pointParser.parseAttributes(el, parent.providerAttributes);
    Kit kit = context.legacy().getKits().parseKitProperty(el, "kit", parent.kit);

    boolean sequential = XMLUtils.parseBoolean(el.getAttribute("sequential"), parent.sequential);
    boolean spread = XMLUtils.parseBoolean(el.getAttribute("spread"), parent.spread);
    boolean exclusive = XMLUtils.parseBoolean(el.getAttribute("exclusive"), parent.exclusive);
    boolean persistent = XMLUtils.parseBoolean(el.getAttribute("persistent"), parent.persistent);

    boolean newFilters = false;
    List<Filter> filters = new ArrayList<>();

    if (parent.filter != StaticFilter.ABSTAIN) {
      filters.add(parent.filter);
    }

    Node nodeTeam = Node.fromAttr(el, "team");
    if (nodeTeam != null) {
      if (!this.context.hasModule(TeamModule.class)) {
        throw new InvalidXMLException("no teams defined", nodeTeam);
      }
      filters.add(new TeamFilter(Teams.getTeamRef(nodeTeam, this.context)));
      newFilters = true;
    }

    Filter filter = this.context.legacy().getFilters().parseFilterProperty(el, "filter");
    if (filter != null) {
      filters.add(filter);
      newFilters = true;
    }

    if (providerAttributes == parent.providerAttributes
        && kit == parent.kit
        && sequential == parent.sequential
        && spread == parent.spread
        && exclusive == parent.exclusive
        && persistent == parent.persistent
        && !newFilters) {

      return parent;
    } else {
      return new SpawnAttributes(
          AllFilter.of(filters),
          providerAttributes,
          kit,
          sequential,
          spread,
          exclusive,
          persistent);
    }
  }
}

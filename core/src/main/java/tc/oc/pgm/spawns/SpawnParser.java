package tc.oc.pgm.spawns;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.party.TeamFilter;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.PointProviderAttributes;
import tc.oc.pgm.points.RandomPointProvider;
import tc.oc.pgm.points.SequentialPointProvider;
import tc.oc.pgm.points.SpreadPointProvider;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class SpawnParser {
  protected final MapFactory factory;
  protected final PointParser pointParser;
  protected @Nullable Spawn defaultSpawn;

  public SpawnParser(MapFactory factory, PointParser pointParser) {
    this.factory = factory;
    this.pointParser = pointParser;
  }

  public @Nullable Spawn getDefaultSpawn() {
    return defaultSpawn;
  }

  public Spawn parse(Element el, SpawnAttributes attributes) throws InvalidXMLException {
    attributes = this.parseAttributes(el, attributes);
    List<PointProvider> providers;

    if (factory.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
      providers = this.pointParser.parse(el, attributes.providerAttributes);
    }
    // Must have <regions>, <region> or region attribute in proto 1.3.6 and above
    else if (el.getChild("regions") != null
        || el.getChild("region") != null
        || el.getAttribute("region") != null) {
      providers =
          new ArrayList<>(
              pointParser.parseMultiProperty(el, attributes.providerAttributes, "region"));
      for (Element elRegions : XMLUtils.getChildren(el, "regions")) {
        providers.addAll(this.pointParser.parseChildren(elRegions, attributes.providerAttributes));
      }
    } else {
      throw new InvalidXMLException(
          "all spawn regions must be enclosed inside <regions>, or use region attribute", el);
    }

    PointProvider provider;
    if (attributes.sequential) {
      provider = new SequentialPointProvider(providers);
    } else if (attributes.spread || attributes.spreadTeammates) {
      provider = new SpreadPointProvider(providers, attributes.spreadTeammates);
    } else {
      provider = new RandomPointProvider(providers);
    }

    Spawn spawn = new Spawn(attributes, provider);
    factory.getFeatures().addFeature(el, spawn);
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
    Kit kit = factory.getKits().parseKitProperty(el, "kit", parent.kit);

    boolean sequential = XMLUtils.parseBoolean(el.getAttribute("sequential"), parent.sequential);
    boolean spread = XMLUtils.parseBoolean(el.getAttribute("spread"), parent.spread);
    boolean spreadTeammates =
        XMLUtils.parseBoolean(el.getAttribute("spread-teammates"), parent.spreadTeammates);
    boolean exclusive = XMLUtils.parseBoolean(el.getAttribute("exclusive"), parent.exclusive);
    boolean persistent = XMLUtils.parseBoolean(el.getAttribute("persistent"), parent.persistent);

    boolean newFilters = false;
    List<Filter> filters = new ArrayList<>();

    if (parent.filter != StaticFilter.ABSTAIN) {
      filters.add(parent.filter);
    }

    Node nodeTeam = Node.fromAttr(el, "team");
    if (nodeTeam != null) {
      if (!this.factory.hasModule(TeamModule.class)) {
        throw new InvalidXMLException("no teams defined", nodeTeam);
      }
      filters.add(new TeamFilter(Teams.getTeamRef(nodeTeam, this.factory)));
      newFilters = true;
    }

    Filter filter = this.factory.getFilters().parseFilterProperty(el, "filter");
    if (filter != null) {
      filters.add(filter);
      newFilters = true;
    }

    if (providerAttributes == parent.providerAttributes
        && kit == parent.kit
        && sequential == parent.sequential
        && spread == parent.spread
        && spreadTeammates == parent.spreadTeammates
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
          spreadTeammates,
          exclusive,
          persistent);
    }
  }
}

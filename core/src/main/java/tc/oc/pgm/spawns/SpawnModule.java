package tc.oc.pgm.spawns;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.text.Component;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class SpawnModule implements MapModule {

  public static final Duration MINIMUM_RESPAWN_DELAY = Duration.ofSeconds(2);
  public static final Duration IGNORE_CLICKS_DELAY = Duration.ofMillis(500);

  protected final Spawn defaultSpawn;
  protected final List<Spawn> spawns;
  protected final List<RespawnOptions> respawnOptions;

  public SpawnModule(Spawn defaultSpawn, List<Spawn> spawns, List<RespawnOptions> respawnOptions) {
    assert defaultSpawn != null;
    this.defaultSpawn = defaultSpawn;
    this.spawns = spawns;
    this.respawnOptions = respawnOptions;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new SpawnMatchModule(match, this);
  }

  public static class Factory implements MapModuleFactory<SpawnModule> {
    private FilterParser filterParser;

    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, KitModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

    @Override
    public SpawnModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      this.filterParser = factory.getFilters();
      SpawnParser parser = new SpawnParser(factory, new PointParser(factory));
      List<Spawn> spawns = Lists.newArrayList();

      for (Element spawnsEl : doc.getRootElement().getChildren("spawns")) {
        spawns.addAll(parser.parseChildren(spawnsEl, new SpawnAttributes()));
      }

      if (parser.getDefaultSpawn() == null) {
        throw new InvalidXMLException("map must have a single default spawn", doc);
      }

      List<RespawnOptions> respawnOptions = Lists.newArrayList();

      for (Element elRespawn :
          XMLUtils.flattenElements(doc.getRootElement(), "respawns", "respawn")) {
        respawnOptions.add(parseOptions(elRespawn, true, false));
      }

      respawnOptions.add(
          parseOptions(
              doc.getRootElement().getChild("respawn"),
              false,
              doc.getRootElement().getChild("autorespawn") == null));

      this.filterParser = null; // Clean up just in case

      return new SpawnModule(parser.getDefaultSpawn(), spawns, respawnOptions);
    }

    private RespawnOptions parseOptions(Element el, boolean filterAllowed, boolean legacy)
        throws InvalidXMLException {
      Duration delay = XMLUtils.parseDuration(el.getAttribute("delay"), MINIMUM_RESPAWN_DELAY);
      boolean auto = XMLUtils.parseBoolean(el.getAttribute("auto"), legacy); // Legacy support
      boolean blackout = XMLUtils.parseBoolean(el.getAttribute("blackout"), false);
      boolean spectate = XMLUtils.parseBoolean(el.getAttribute("spectate"), false);
      boolean bedSpawn = XMLUtils.parseBoolean(el.getAttribute("bed"), false);
      Component message = XMLUtils.parseFormattedText(el, "message");
      Filter filter = filterParser.parseFilterProperty(el, "filter", StaticFilter.ALLOW);

      if (filter != null && !filterAllowed)
        throw new InvalidXMLException("Legacy respawn elements cannot use filters", el);

      if (TimeUtils.isShorterThan(delay, MINIMUM_RESPAWN_DELAY)) {
        if (!legacy)
          throw new InvalidXMLException(
              "Spawn delay must be at least " + MINIMUM_RESPAWN_DELAY.getSeconds() + "s", el);

        delay = MINIMUM_RESPAWN_DELAY;
      }

      return new RespawnOptions(delay, auto, blackout, spectate, bedSpawn, message, filter);
    }
  }
}

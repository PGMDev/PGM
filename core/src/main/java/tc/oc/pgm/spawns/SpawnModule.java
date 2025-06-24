package tc.oc.pgm.spawns;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class SpawnModule implements MapModule<SpawnMatchModule> {

  public static final Duration DEFAULT_RESPAWN_DELAY = Duration.ofMillis(2000);
  public static final Duration MINIMUM_RESPAWN_DELAY = Duration.ZERO;
  public static final Duration IGNORE_CLICKS_DELAY = Duration.ofMillis(500);
  public static final Duration MIN_KIT_DELAY = Duration.ofMillis(1000);

  protected final Spawn defaultSpawn;
  protected final List<Spawn> spawns;
  protected final List<Kit> playerKits;
  protected final List<RespawnOptions> respawnOptions;

  public SpawnModule(
      Spawn defaultSpawn,
      List<Spawn> spawns,
      List<Kit> playerKits,
      List<RespawnOptions> respawnOptions) {
    this.playerKits = playerKits;
    assert defaultSpawn != null;
    this.defaultSpawn = defaultSpawn;
    this.spawns = spawns;
    this.respawnOptions = respawnOptions;
  }

  @Override
  public SpawnMatchModule createMatchModule(Match match) {
    return new SpawnMatchModule(match, this);
  }

  public static class Factory implements MapModuleFactory<SpawnModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, KitModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

    @Override
    public SpawnModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      FilterParser filterParser = factory.getFilters();
      SpawnParser parser = new SpawnParser(factory, new PointParser(factory));
      List<Spawn> spawns = Lists.newArrayList();
      List<Kit> playerKits = new ArrayList<>();

      for (Element spawnsEl : doc.getRootElement().getChildren("spawns")) {
        spawns.addAll(parser.parseChildren(spawnsEl, new SpawnAttributes()));
        final Kit playerKit = factory.getKits().parseKitProperty(spawnsEl, "player-kit");
        if (playerKit != null) {
          playerKits.add(playerKit);
        }
      }

      if (parser.getDefaultSpawn() == null) {
        throw new InvalidXMLException("map must have a single default spawn", doc);
      }

      return new SpawnModule(
          parser.getDefaultSpawn(), spawns, playerKits, parseRespawnOptions(doc, filterParser));
    }

    private List<RespawnOptions> parseRespawnOptions(Document doc, FilterParser filterParser)
        throws InvalidXMLException {
      List<RespawnOptions> respawnOptions = Lists.newArrayList();

      for (Element elRespawn :
          XMLUtils.flattenElements(doc.getRootElement(), "respawns", "respawn")) {
        respawnOptions.add(getRespawnOptions(elRespawn, filterParser));
      }
      // Parse root children respawn elements, Keeps old syntax and gives a default spawn if all
      // others fail
      respawnOptions.add(getRespawnOptions(
          doc.getRootElement().getChildren("respawn"),
          doc.getRootElement().getChild("autorespawn") != null,
          true,
          filterParser));

      return respawnOptions;
    }

    private RespawnOptions getRespawnOptions(Element element, FilterParser filterParser)
        throws InvalidXMLException {
      return getRespawnOptions(Collections.singleton(element), false, false, filterParser);
    }

    protected RespawnOptions getRespawnOptions(
        Collection<Element> elements,
        boolean autorespawn,
        boolean topLevel,
        FilterParser filterParser)
        throws InvalidXMLException {
      Duration delay = DEFAULT_RESPAWN_DELAY;
      boolean auto = autorespawn;
      boolean blackout = false;
      boolean spectate = false;
      boolean bedSpawn = false;
      Filter filter = StaticFilter.ALLOW;
      Component message = null;

      for (Element elRespawn : elements) {
        delay = XMLUtils.parseDuration(elRespawn.getAttribute("delay"), delay);
        auto = XMLUtils.parseBoolean(elRespawn.getAttribute("auto"), auto);
        blackout = XMLUtils.parseBoolean(elRespawn.getAttribute("blackout"), blackout);
        spectate = XMLUtils.parseBoolean(elRespawn.getAttribute("spectate"), spectate);
        bedSpawn = XMLUtils.parseBoolean(elRespawn.getAttribute("bed"), bedSpawn);
        filter = filterParser.parseFilterProperty(elRespawn, "filter", filter);
        if (filter != StaticFilter.ALLOW && topLevel)
          throw new InvalidXMLException("Parent respawn elements can't use filters", elRespawn);

        message = XMLUtils.parseFormattedText(elRespawn, "message", message);

        if (TimeUtils.isShorterThan(delay, MINIMUM_RESPAWN_DELAY)) delay = MINIMUM_RESPAWN_DELAY;
      }

      return new RespawnOptions(delay, auto, blackout, spectate, bedSpawn, filter, message);
    }
  }
}

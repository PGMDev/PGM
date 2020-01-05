package tc.oc.pgm.spawns;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.points.PointParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "spawns",
    depends = {RegionModule.class, KitModule.class},
    follows = {TeamModule.class})
public class SpawnModule extends MapModule<SpawnMatchModule> {

  public static final Duration MINIMUM_RESPAWN_DELAY = Duration.standardSeconds(2);
  public static final Duration IGNORE_CLICKS_DELAY = Duration.millis(500);

  protected final Spawn defaultSpawn;
  protected final List<Spawn> spawns;
  protected final RespawnOptions respawnOptions;

  public SpawnModule(Spawn defaultSpawn, List<Spawn> spawns, RespawnOptions respawnOptions) {
    assert defaultSpawn != null;
    this.defaultSpawn = defaultSpawn;
    this.spawns = spawns;
    this.respawnOptions = respawnOptions;
  }

  @Override
  public SpawnMatchModule createMatchModule(Match match) {
    return new SpawnMatchModule(match, this);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static SpawnModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    SpawnParser parser = new SpawnParser(context, new PointParser(context));
    List<Spawn> spawns = Lists.newArrayList();

    for (Element spawnsEl : doc.getRootElement().getChildren("spawns")) {
      spawns.addAll(parser.parseChildren(spawnsEl, new SpawnAttributes()));
    }

    if (parser.getDefaultSpawn() == null) {
      throw new InvalidXMLException("map must have a single default spawn", doc);
    }

    return new SpawnModule(
        parser.getDefaultSpawn(), spawns, parseRespawnOptions(context, logger, doc));
  }

  protected static RespawnOptions parseRespawnOptions(
      MapModuleContext context, Logger logger, Document doc) throws InvalidXMLException {
    Duration delay = MINIMUM_RESPAWN_DELAY;
    boolean auto = doc.getRootElement().getChild("autorespawn") != null; // Legacy support
    boolean blackout = false;
    boolean spectate = false;
    boolean bedSpawn = false;
    Component message = null;

    for (Element elRespawn : doc.getRootElement().getChildren("respawn")) {
      delay = XMLUtils.parseDuration(elRespawn.getAttribute("delay"), delay);
      auto = XMLUtils.parseBoolean(elRespawn.getAttribute("auto"), auto);
      blackout = XMLUtils.parseBoolean(elRespawn.getAttribute("blackout"), blackout);
      spectate = XMLUtils.parseBoolean(elRespawn.getAttribute("spectate"), spectate);
      bedSpawn = XMLUtils.parseBoolean(elRespawn.getAttribute("bed"), bedSpawn);
      message = XMLUtils.parseFormattedText(elRespawn, "message", message);

      if (delay.isShorterThan(MINIMUM_RESPAWN_DELAY)) delay = MINIMUM_RESPAWN_DELAY;
    }

    return new RespawnOptions(delay, auto, blackout, spectate, bedSpawn, message);
  }

  @Override
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    // TODO: Make this feasible and remove null checks in the spawn module
    // for(Spawn spawn : spawns) {
    //    if(spawn.pointProvider.canFail()) {
    //        throw new InvalidXMLException("Spawn is not guaranteed to provide a spawning
    // location", context.features().getNode(spawn));
    //    }
    // }
  }
}

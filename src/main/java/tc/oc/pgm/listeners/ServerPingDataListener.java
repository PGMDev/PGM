package tc.oc.pgm.listeners;

import static com.google.common.base.Preconditions.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.map.Contributor;
import tc.oc.pgm.map.MapInfo;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.rotation.PGMMapOrder;
import tc.oc.util.logging.ClassLogger;

public class ServerPingDataListener implements Listener {

  private final MatchManager matchManager;
  private final Logger logger;
  private final AtomicBoolean ready;
  private final AtomicBoolean legacySportPaper;
  private final LoadingCache<Match, JsonObject> matchCache;

  public ServerPingDataListener(MatchManager matchManager, Logger parentLogger) {
    this.matchManager = checkNotNull(matchManager);
    this.logger = ClassLogger.get(checkNotNull(parentLogger), ServerPingDataListener.class);
    this.ready = new AtomicBoolean();
    this.legacySportPaper = new AtomicBoolean();
    this.matchCache =
        CacheBuilder.newBuilder()
            .weakKeys()
            .expireAfterWrite(5L, TimeUnit.SECONDS)
            .build(
                new CacheLoader<Match, JsonObject>() {
                  @Override
                  public JsonObject load(Match match) throws Exception {
                    JsonObject jsonObject = new JsonObject();
                    serializeMatch(match, jsonObject);
                    return jsonObject;
                  }
                });
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    ready.compareAndSet(false, true);
  }

  @EventHandler
  public void onServerListPing(ServerListPingEvent event) {
    if (!ready.get() || legacySportPaper.get()) return;

    try {
      JsonObject root = event.getOrCreateExtra(PGM.get());
      for (Match match : this.matchManager.getMatches()) {
        String matchId = match.getId();
        try {
          root.add(matchId, this.matchCache.get(match));
        } catch (ExecutionException e) {
          this.logger.log(Level.SEVERE, "Could not load server ping data for match: " + matchId, e);
        }
      }
    } catch (NoSuchMethodError ex) {
      legacySportPaper.compareAndSet(false, true);
    }
  }

  private void serializeMatch(Match match, JsonObject jsonObject) {
    checkNotNull(match);
    checkNotNull(jsonObject);

    jsonObject.addProperty("state", match.getPhase().toString());
    jsonObject.addProperty("duration", match.getDuration().getStandardSeconds());
    jsonObject.addProperty("observers", match.getObservers().size());
    jsonObject.addProperty("participants", match.getParticipants().size());
    jsonObject.addProperty("max_players", match.getMaxPlayers());

    JsonObject mapObject = new JsonObject();
    this.serializeMapInfo(match.getMap().getInfo(), mapObject);
    jsonObject.add("map", mapObject);

    this.appendNextMap(jsonObject);
  }

  private void appendNextMap(JsonObject jsonObject) {
    checkNotNull(jsonObject);

    PGMMapOrder mapOrder = this.matchManager.getMapOrder();
    if (mapOrder != null) {
      PGMMap nextMap = mapOrder.getNextMap();

      if (nextMap != null) {
        JsonObject nextMapObject = new JsonObject();
        this.serializeMapInfo(nextMap.getInfo(), nextMapObject);
        jsonObject.add("next_map", nextMapObject);
      }
    }
  }

  private void serializeMapInfo(MapInfo mapInfo, JsonObject jsonObject) {
    checkNotNull(mapInfo);
    checkNotNull(jsonObject);

    jsonObject.addProperty("slug", mapInfo.slug());
    jsonObject.addProperty("name", mapInfo.name);
    jsonObject.addProperty("version", mapInfo.version.toString());
    jsonObject.addProperty("objective", mapInfo.objective);

    JsonArray authors = new JsonArray();
    for (Contributor author : mapInfo.authors) {
      JsonObject authorObject = new JsonObject();
      this.serializeContributor(author, authorObject);
      authors.add(authorObject);
    }
    if (authors.iterator().hasNext()) {
      jsonObject.add("authors", authors);
    }

    JsonArray contributors = new JsonArray();
    for (Contributor contributor : mapInfo.contributors) {
      JsonObject contributorObject = new JsonObject();
      this.serializeContributor(contributor, contributorObject);
      contributors.add(contributorObject);
    }
    if (contributors.iterator().hasNext()) {
      jsonObject.add("contributors", contributors);
    }
  }

  private void serializeContributor(Contributor contributor, JsonObject jsonObject) {
    checkNotNull(contributor, "contributor");
    checkNotNull(jsonObject, "jsonObject");

    UUID playerId = contributor.getPlayerId();
    if (playerId != null) {
      jsonObject.addProperty("uuid", playerId.toString());
    }

    if (contributor.hasName()) {
      jsonObject.addProperty("name", contributor.getName());
    }

    if (contributor.hasContribution()) {
      jsonObject.addProperty("contribution", contributor.getContribution());
    }
  }
}

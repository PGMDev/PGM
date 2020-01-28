package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchUnloadEvent;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.util.ClassLogger;

public class MatchManagerImpl implements MatchManager, Listener {

  private final Logger logger;
  private final Map<String, Match> matchById;
  private final Map<String, Match> matchByWorld;

  public MatchManagerImpl(Logger logger) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass());
    this.matchById = Collections.synchronizedMap(new LinkedHashMap<>());
    this.matchByWorld = new HashMap<>();
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    final Match match = event.getMatch();

    matchById.put(checkNotNull(match).getId(), match);
    matchByWorld.put(checkNotNull(match.getWorld()).getName(), match);

    logger.info("Loaded match-" + match.getId() + " (" + match.getMap().getId() + ")");
  }

  @EventHandler
  public void onMatchUnload(MatchUnloadEvent event) {
    final Match match = event.getMatch();

    matchById.remove(checkNotNull(match).getId());
    matchByWorld.remove(checkNotNull(match.getWorld()).getName());
    PGM.get()
        .getServer()
        .getScheduler()
        .runTaskLaterAsynchronously(
            PGM.get(), match::destroy, Config.Experiments.get().getMatchDestroySeconds() * 20);

    logger.info("Unloaded match-" + match.getId() + " (" + match.getMap().getId() + ")");
  }

  @Override
  public MatchFactory createMatch(@Nullable String mapId) {
    // FIXME: "infinite" retry if a Match fails to load
    if (mapId == null) mapId = PGM.get().getMapOrder().popNextMap().getId();
    return new MatchFactoryImpl(mapId);
  }

  @Override
  public Match getMatch(@Nullable World world) {
    return matchByWorld.get(world == null ? null : world.getName());
  }

  @Override
  public Iterator<Match> getMatches() {
    return Iterators.unmodifiableIterator(matchById.values().iterator());
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return Iterables.unmodifiableIterable(matchById.values());
  }

  @Override
  public MatchPlayer getPlayer(@Nullable Player bukkit) {
    if (bukkit == null) return null;
    final Match match = getMatch(bukkit.getWorld());
    if (match == null) return null;
    return match.getPlayer(bukkit);
  }
}

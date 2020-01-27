package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchManagerImpl implements MatchManager, Listener {

  private final Map<String, Match> matchById;
  private final Map<String, Match> matchByWorld;

  public MatchManagerImpl() {
    this.matchById = new ConcurrentHashMap<>();
    this.matchByWorld = new ConcurrentHashMap<>();
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    final Match match = event.getMatch();

    matchById.put(checkNotNull(match).getId(), match);
    matchByWorld.put(checkNotNull(match.getWorld()).getName(), match);
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
  public Iterable<Match> getMatches() {
    return Iterables.filter(matchById.values(), Match::isLoaded);
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return getMatches();
  }

  @Override
  public MatchPlayer getPlayer(@Nullable Player bukkit) {
    // FIXME: determine if this needs to be more efficient with N matches
    for (Match match : getMatches()) {
      final MatchPlayer player = match.getPlayer(bukkit);
      if (player != null) {
        return player;
      }
    }
    return null;
  }
}

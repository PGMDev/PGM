package tc.oc.pgm.match;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.bukkit.World;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MatchManagerImpl implements MatchManager {

  private final Map<String, Match> matchById;
  private final Map<String, Match> matchByWorld;

  public MatchManagerImpl() {
    this.matchById = new HashMap<>();
    this.matchByWorld = new HashMap<>();
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
  public void addMatch(Match match) {
    matchById.put(checkNotNull(match).getId(), match);
    matchByWorld.put(match.getWorld().getName(), match);

    PGM.get()
        .getServer()
        .getScheduler()
        .runTaskLaterAsynchronously(PGM.get(), this::cleanUp, 20 * 10 /* 10 seconds later */);
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return getMatches();
  }

  @Override
  public MatchPlayer getPlayer(@Nullable Player bukkit) {
    // TODO: determine if this needs to be more efficient with N matches
    for (Match match : getMatches()) {
      final MatchPlayer player = match.getPlayer(bukkit);
      if (player != null) {
        return player;
      }
    }
    return null;
  }

  private void cleanUp() {
    for (Match match : ImmutableList.copyOf(matchById.values())) {
      if (match.getWorld() == null || !match.isLoaded()) {
        match.destroy();

        matchById.remove(match.getId());
        for (Map.Entry<String, Match> entry : ImmutableList.copyOf(matchByWorld.entrySet())) {
          if (match.equals(entry.getValue())) {
            matchByWorld.remove(entry.getKey());
          }
        }
      }
    }
  }
}

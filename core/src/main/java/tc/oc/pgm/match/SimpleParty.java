package tc.oc.pgm.match;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.filters.query.PartyQuery;
import tc.oc.util.bukkit.chat.Audience;
import tc.oc.util.bukkit.chat.MultiAudience;

/** Represents a simple {@link Party} with a set of {@link MatchPlayer}s. */
public abstract class SimpleParty implements Party, MultiAudience {

  protected final Match match;
  protected final Map<UUID, MatchPlayer> players = new ConcurrentHashMap<>();
  protected final PartyQuery query = new PartyQuery(null, this);

  public SimpleParty(Match match) {
    this.match = match;
  }

  @Override
  public Match getMatch() {
    return match;
  }

  @Override
  public IPartyQuery getQuery() {
    return query;
  }

  @Override
  public void internalAddPlayer(MatchPlayer player) {
    players.put(player.getId(), player);
  }

  @Override
  public void internalRemovePlayer(MatchPlayer player) {
    players.remove(player.getId());
  }

  @Override
  public Collection<MatchPlayer> getPlayers() {
    return Collections.unmodifiableCollection(players.values());
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return getPlayers();
  }

  @Override
  public @Nullable MatchPlayer getPlayer(UUID playerId) {
    return players.get(playerId);
  }
}

package tc.oc.pgm.match;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.chat.Sound;
import tc.oc.component.Component;
import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.filters.query.PartyQuery;

public abstract class MultiPlayerParty implements Party {

  protected final Match match;
  protected final Set<MatchPlayer> players = new HashSet<>();
  protected final PartyQuery query = new PartyQuery(null, this);

  public MultiPlayerParty(Match match) {
    this.match = match;
  }

  /**
   * Gets the match that this team is tied to.
   *
   * @return Match this team is tied to.
   */
  @Override
  public Match getMatch() {
    return this.match;
  }

  @Override
  public IPartyQuery getQuery() {
    return query;
  }

  @Override
  public boolean addPlayer(MatchPlayer player) {
    return players.add(player);
  }

  @Override
  public boolean removePlayer(MatchPlayer player) {
    return players.remove(player);
  }

  @Override
  public Set<MatchPlayer> getPlayers() {
    return players;
  }

  /**
   * Return the member of this team matching the given ID, or null if there is no matching player
   * currently on this team.
   */
  @Override
  public @Nullable MatchPlayer getPlayer(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return null;
    for (MatchPlayer teamPlayer : this.getPlayers()) {
      if (player == teamPlayer.getBukkit()) return teamPlayer;
    }
    return null;
  }

  /**
   * Send a specific message that may include colors to all members of this team.
   *
   * @param message Message to send.
   */
  @Override
  public void sendMessage(String message) {
    for (MatchPlayer player : this.getPlayers()) {
      player.sendMessage(message);
    }
  }

  @Override
  public void sendMessage(Component message) {
    for (MatchPlayer player : this.getPlayers()) {
      player.sendMessage(message);
    }
  }

  @Override
  public void sendWarning(String message, boolean audible) {
    for (MatchPlayer player : this.getPlayers()) {
      player.sendWarning(message, audible);
    }
  }

  @Override
  public void sendWarning(Component message, boolean audible) {
    for (MatchPlayer player : this.getPlayers()) {
      player.sendWarning(message, audible);
    }
  }

  @Override
  public void playSound(Sound sound) {
    for (MatchPlayer player : this.getPlayers()) {
      player.playSound(sound);
    }
  }

  @Override
  public void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    for (MatchPlayer player : this.getPlayers()) {
      player.showTitle(title, subtitle, inTicks, stayTicks, outTicks);
    }
  }

  @Override
  public void sendHotbarMessage(Component message) {
    for (MatchPlayer player : this.getPlayers()) {
      player.sendHotbarMessage(message);
    }
  }
}

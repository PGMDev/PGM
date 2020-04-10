package tc.oc.pgm.ffa;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.util.bukkit.BukkitUtils;
import tc.oc.util.bukkit.chat.Audience;
import tc.oc.util.bukkit.chat.MultiAudience;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedPlayer;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.named.NameStyle;

/**
 * Wraps a single {@link MatchPlayer} in a free-for-all match.
 *
 * <p>A Tribute is created on demand for a player the first time they join a match. It is initially
 * "empty", and the player has to be added to it, in the same way they are added to any party.
 *
 * <p>If the player leaves the match, they will be removed from the Tribute, and the empty Tribute
 * will be removed from the match. As with any inactive {@link Competitor}, the Tribute is retained
 * by {@link Match}, indexed by {@link #getId}, which in this case is the player's ID. If the player
 * rejoins the same match, the FFA module will retrieve their existing Tribute and add them back to
 * it, instead of creating a new one.
 *
 * <p>Attempting to add the wrong player, or add multiple players, will throw {@link
 * UnsupportedOperationException}.
 */
public class Tribute implements Competitor, MultiAudience {

  public static final ChatColor TEXT_COLOR = ChatColor.YELLOW;

  protected final Match match;
  protected final FreeForAllMatchModule ffa;
  protected final UUID playerId;
  protected final String username;
  protected final ChatColor color;
  protected final tc.oc.pgm.filters.query.PartyQuery query =
      new tc.oc.pgm.filters.query.PartyQuery(null, this);

  protected @Nullable MatchPlayer player;
  protected Set<MatchPlayer> players = Collections.emptySet();

  public Tribute(MatchPlayer player, @Nullable ChatColor color) {
    this.match = player.getMatch();
    this.ffa = match.needModule(FreeForAllMatchModule.class);

    this.playerId = player.getId();
    this.username = player.getBukkit().getName();
    this.color = color == null ? TEXT_COLOR : color;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{match=" + getMatch() + ", name=" + getName() + "}";
  }

  public UUID getPlayerId() {
    return playerId;
  }

  @Override
  public Match getMatch() {
    return match;
  }

  @Override
  public String getId() {
    return playerId.toString();
  }

  @Override
  public String getDefaultName() {
    return getName();
  }

  @Override
  public String getName() {
    return username;
  }

  @Override
  public String getName(@Nullable CommandSender viewer) {
    return username;
  }

  @Override
  public boolean isNamePlural() {
    return false;
  }

  @Override
  public ChatColor getColor() {
    return color;
  }

  @Override
  public Color getFullColor() {
    return BukkitUtils.colorOf(color);
  }

  @Override
  public String getColoredName() {
    return getColor() + getName();
  }

  @Override
  public String getColoredName(@Nullable CommandSender viewer) {
    return getColor() + getName(viewer);
  }

  @Override
  public Component getComponentName() {
    return new PersonalizedText(getColoredName());
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return new PersonalizedPlayer(player == null ? null : player.getBukkit(), username, style);
  }

  @Override
  public Component getChatPrefix() {
    return new PersonalizedText();
  }

  @Override
  public boolean isParticipating() {
    return getMatch().isRunning();
  }

  @Override
  public boolean isObserving() {
    return !getMatch().isRunning();
  }

  @Override
  public NameTagVisibility getNameTagVisibility() {
    return ffa.getOptions().nameTagVisibility;
  }

  @Override
  public Set<MatchPlayer> getPlayers() {
    return players;
  }

  @Override
  public @Nullable MatchPlayer getPlayer(UUID playerId) {
    return player != null && player.getId().equals(playerId) ? player : null;
  }

  protected void checkPlayer(MatchPlayer player) {
    if (!player.getId().equals(playerId)) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void internalAddPlayer(MatchPlayer player) {
    checkPlayer(player);
    this.player = player;
    this.players = Collections.singleton(player);
  }

  @Override
  public void internalRemovePlayer(MatchPlayer player) {
    checkPlayer(player);
    this.player = null;
    this.players = Collections.emptySet();
  }

  @Override
  public boolean isAutomatic() {
    return true;
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return player == null ? Collections.emptyList() : Collections.singleton(player);
  }

  /**
   * If the player is online and participating, this delegates to {@link MatchPlayer#getQuery()}.
   * Otherwise it returns an {@link PlayerQuery}, which knows about the player's identity, but has
   * no properties related to physical presence in the match.
   */
  @Override
  public PartyQuery getQuery() {
    return player != null ? (PartyQuery) player.getQuery() : query;
  }
}

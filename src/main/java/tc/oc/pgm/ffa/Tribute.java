package tc.oc.pgm.ffa;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.component.types.PersonalizedText;
import tc.oc.identity.Identities;
import tc.oc.named.NameStyle;
import tc.oc.named.Names;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.pgm.filters.query.PartyQuery;

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
public class Tribute implements Competitor {

  public static final ChatColor TEXT_COLOR = ChatColor.YELLOW;

  protected final Match match;
  protected final FreeForAllMatchModule ffa;
  protected final UUID playerId;
  protected final String username;
  protected final PartyQuery query = new PartyQuery(null, this);

  protected @Nullable MatchPlayer player;
  protected Set<MatchPlayer> players = Collections.emptySet();

  public Tribute(MatchPlayer player) {
    this.match = player.getMatch();
    this.ffa = match.needModule(FreeForAllMatchModule.class);

    this.playerId = player.getId();
    this.username = player.getBukkit().getName();
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
    return Names.text(player.getBukkit(), viewer);
  }

  @Override
  public boolean isNamePlural() {
    return false;
  }

  @Override
  public ChatColor getColor() {
    return TEXT_COLOR;
  }

  @Override
  public Color getFullColor() {
    return Color.ORANGE;
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
    return getStyledName(NameStyle.COLOR);
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return new PersonalizedPlayer(Identities.current(player.getBukkit()), style);
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
  public void sendMessage(String message) {
    if (player != null) player.sendMessage(message);
  }

  @Override
  public void sendMessage(Component message) {
    if (player != null) player.sendMessage(message);
  }

  @Override
  public void sendWarning(String message, boolean audible) {
    if (player != null) player.sendWarning(message, audible);
  }

  @Override
  public void sendWarning(Component message, boolean audible) {
    if (player != null) player.sendWarning(message, audible);
  }

  @Override
  public void playSound(Sound sound) {
    if (player != null) player.playSound(sound);
  }

  @Override
  public void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    if (player != null) player.showTitle(title, subtitle, inTicks, stayTicks, outTicks);
  }

  @Override
  public void sendHotbarMessage(Component message) {
    if (player != null) player.sendHotbarMessage(message);
  }

  /**
   * If the player is online and participating, this delegates to {@link MatchPlayer#getQuery()}.
   * Otherwise it returns an {@link IPlayerQuery}, which knows about the player's identity, but has
   * no properties related to physical presence in the match.
   */
  @Override
  public IPartyQuery getQuery() {
    return player != null ? (IPartyQuery) player.getQuery() : query;
  }
}

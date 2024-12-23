package tc.oc.pgm.ffa;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.PartyQuery;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

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
 * <p>Attempting to add the wrong player, or add multiple players, will throw
 * {@link UnsupportedOperationException}.
 */
public class Tribute implements Competitor {

  private final Match match;
  private final FreeForAllMatchModule ffa;

  private final UUID id;
  private final String username;
  private final ChatColor chatColor;
  private final Color color;
  private final DyeColor dyeColor;
  private final NamedTextColor textColor;
  private final PartyQuery query;
  private NameTagVisibility nameTagOverride;

  protected @Nullable MatchPlayer player;
  protected List<MatchPlayer> players = Collections.emptyList();

  public Tribute(final MatchPlayer player, final @Nullable ChatColor color) {
    this.match = player.getMatch();
    this.ffa = match.needModule(FreeForAllMatchModule.class);
    this.id = player.getId();
    this.username = player.getBukkit().getName();
    this.chatColor = color == null ? ChatColor.YELLOW : color;
    this.color = BukkitUtils.colorOf(this.chatColor);
    this.dyeColor = BukkitUtils.chatColorToDyeColor(this.chatColor);
    this.textColor = TextFormatter.convert(chatColor);
    this.query = new PartyQuery(null, this);
  }

  @Override
  public Match getMatch() {
    return this.match;
  }

  @Override
  public String getId() {
    return this.username;
  }

  @Override
  public String getDefaultName() {
    return this.username;
  }

  @Override
  public boolean isNamePlural() {
    return false;
  }

  @Override
  public ChatColor getColor() {
    return this.chatColor;
  }

  @Override
  public Color getFullColor() {
    return this.color;
  }

  @Override
  public DyeColor getDyeColor() {
    return dyeColor;
  }

  @Override
  public NamedTextColor getTextColor() {
    return this.textColor;
  }

  @Override
  public Component getName(final NameStyle style) {
    return player(player, style);
  }

  @Override
  public String getNameLegacy() {
    return username;
  }

  @Override
  public Component getChatPrefix() {
    return Component.empty();
  }

  @Override
  public boolean isParticipating() {
    return this.getMatch().isRunning();
  }

  @Override
  public boolean isObserving() {
    return !this.getMatch().isRunning();
  }

  @Override
  public NameTagVisibility getNameTagVisibility() {
    return this.nameTagOverride != null ? nameTagOverride : ffa.getOptions().nameTagVisibility;
  }

  @Override
  public void setNameTagVisibility(NameTagVisibility override) {
    this.nameTagOverride = override;
  }

  @Override
  public Collection<MatchPlayer> getPlayers() {
    return this.players;
  }

  @Nullable
  public MatchPlayer getPlayer() {
    return this.player;
  }

  @Override
  public @Nullable MatchPlayer getPlayer(final UUID playerId) {
    return player != null && player.getId().equals(playerId) ? player : null;
  }

  private void checkPlayer(final UUID playerId) {
    if (!this.id.equals(playerId)) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void addPlayer(final MatchPlayer player) {
    checkPlayer(assertNotNull(player).getId());
    this.player = player;
    this.players = Collections.singletonList(player);
  }

  @Override
  public void removePlayer(final UUID playerId) {
    checkPlayer(playerId);
    this.player = null;
    this.players = Collections.emptyList();
  }

  @Override
  public boolean isAutomatic() {
    return true;
  }

  @Override
  public net.kyori.adventure.audience.@NotNull Audience audience() {
    return player != null ? this.player : Audience.empty();
  }

  /**
   * If the player is online and participating, this delegates to that player. Otherwise it returns
   * a PlayerQuery, which knows about the player's identity, but has no properties related to
   * physical presence in the match.
   */
  @Override
  public tc.oc.pgm.api.filter.query.PartyQuery getQuery() {
    return player != null ? player : query;
  }

  @Override
  public void setName(final String name) {
    throw new UnsupportedOperationException("Cannot rename tribute: " + name);
  }
}

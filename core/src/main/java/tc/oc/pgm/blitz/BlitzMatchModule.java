package tc.oc.pgm.blitz;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;

import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

@ListenerScope(MatchScope.RUNNING)
public class BlitzMatchModule implements MatchModule, Listener {

  private final Match match;
  private final BlitzConfig config;
  private final LifeManager lifeManager;
  private final Set<UUID> eliminatedPlayers = new HashSet<>();

  public BlitzMatchModule(Match match, BlitzConfig config) {
    this.match = match;
    this.config = config;
    this.lifeManager = new LifeManager(this.config.getNumLives());
  }

  @Override
  public void load() {
    match.addVictoryCondition(new BlitzVictoryCondition());
  }

  public BlitzConfig getConfig() {
    return this.config;
  }

  public Filter getScoreboardFilter() {
    return config.getScoreboardFilter();
  }

  /** Whether or not the player participated in the match and was eliminated. */
  public boolean isPlayerEliminated(UUID player) {
    return this.eliminatedPlayers.contains(player);
  }

  public int getRemainingPlayers(Competitor competitor) {
    // TODO: this becomes a bit more complex when eliminated players are not forced to observers
    return competitor.getPlayers().size();
  }

  public int getNumOfLives(UUID id) {
    return lifeManager.getLives(id);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void handleDeath(final MatchPlayerDeathEvent event) {
    MatchPlayer victim = event.getVictim();
    if (config.getFilter().query(victim).isDenied()) return;
    if (victim.getParty() instanceof Competitor) {
      Competitor competitor = (Competitor) victim.getParty();

      int lives = this.lifeManager.addLives(event.getVictim().getId(), -1);

      if (lives <= 0) {
        this.handleElimination(victim, competitor);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void handleLeave(final PlayerPartyChangeEvent event) {
    int lives = this.lifeManager.getLives(event.getPlayer().getId());
    if (event.getOldParty() instanceof Competitor && lives > 0) {
      if (event.getNewParty() != null && event.getNewParty() instanceof Competitor) {
        // Player switching teams, check if match needs to end
        checkEnd();
      } else if (config.getFilter().query(event.getPlayer()).isAllowed()) {
        // Player is going to obs and filter allows, eliminate them
        this.handleElimination(event.getPlayer(), (Competitor) event.getOldParty());
      }
    }
  }

  public boolean canJoin(MatchPlayer player, @Nullable JoinRequest request) {
    if (isPlayerEliminated(player.getId())) return false;
    if (!match.isRunning() || (request != null && request.has(JoinRequest.Flag.FORCE))) return true;

    return config.getJoinFilter().query(player.getMatch()).isAllowed();
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void handleJoin(final PlayerParticipationStartEvent event) {
    if (canJoin(event.getPlayer(), event.getRequest())) return;

    event.cancel(
        translatable("blitz.joinDenied", text(Gamemode.BLITZ.getFullName(), NamedTextColor.AQUA)));
  }

  @EventHandler
  public void handleSpawn(final ParticipantSpawnEvent event) {
    if (this.config.getBroadcastLives()) {
      int lives = this.lifeManager.getLives(event.getPlayer().getId());
      event
          .getPlayer()
          .showTitle(
              title(
                  empty(),
                  translatable(
                      "blitz.livesRemaining",
                      NamedTextColor.RED,
                      translatable(
                          lives == 1 ? "misc.life" : "misc.lives",
                          NamedTextColor.AQUA,
                          text(lives))),
                  Title.Times.times(Duration.ZERO, fromTicks(60), fromTicks(20))));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBlitzPlayerEliminated(final BlitzPlayerEliminatedEvent event) {
    this.eliminatedPlayers.add(event.getPlayer().getBukkit().getUniqueId());

    World world = event.getMatch().getWorld();
    Location death = event.getDeathLocation();

    double radius = 0.1;
    int n = 8;
    for (int i = 0; i < 6; i++) {
      double angle = 2 * Math.PI * i / n;
      Location base =
          death.clone().add(new Vector(radius * Math.cos(angle), 0, radius * Math.sin(angle)));
      for (int j = 0; j <= 8; j++) {
        world.playEffect(base, Effect.SMOKE, j);
      }
    }
  }

  private void handleElimination(final MatchPlayer player, Competitor competitor) {
    if (!eliminatedPlayers.add(player.getBukkit().getUniqueId())) return;

    match.callEvent(
        new BlitzPlayerEliminatedEvent(player, competitor, player.getBukkit().getLocation()));

    checkEnd();
  }

  private void checkEnd() {
    // Process eliminations within the same tick simultaneously, so that ties are properly detected

    // Player leaving may have ended the match, causing a rejected execution.
    if (!match.isRunning()) return;

    match
        .getExecutor(MatchScope.RUNNING)
        .execute(
            () -> {
              ImmutableSet.copyOf(match.getParticipants()).stream()
                  .filter(
                      participating -> isPlayerEliminated(participating.getBukkit().getUniqueId()))
                  .forEach(participating -> match.setParty(participating, match.getDefaultParty()));

              match.calculateVictory();
            });
  }
}

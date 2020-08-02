package tc.oc.pgm.api;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.permissions.Permission;
import tc.oc.pgm.api.map.factory.MapSourceFactory;

/** A configuration for server owners to modify {@link PGM}. */
public interface Config {

  /**
   * Gets the log level to set in console.
   *
   * @return A log level, info is recommended.
   */
  Level getLogLevel();

  /**
   * Gets a resource identifier for a database.
   *
   * <p>eg. sqlite3://path/to/pgm.db, mysql://localhost?ssl=false
   *
   * @return A database uri, or null to use an in-memory database.
   */
  @Nullable
  String getDatabaseUri();

  /**
   * Gets the maximum number of concurrent database connections.
   *
   * @return Number of connections.
   */
  int getDatabaseMaxConnections();

  /**
   * Gets a list of map source factories
   *
   * @return A list of map source factories.
   */
  List<? extends MapSourceFactory> getMapSourceFactories();

  /**
   * Gets a path to "map-pool.yml" file.
   *
   * @return A path to a map pool, or null for no map pools.
   */
  @Nullable
  String getMapPoolFile();

  /**
   * Gets a duration to wait before starting a match.
   *
   * @return A duration, if non-positive then starts immediately.
   */
  Duration getStartTime();

  /**
   * Gets a duration to give teams to "strategize" before the match starts.
   *
   * @return A duration, if non-positive or null then skips this phase.
   */
  Duration getHuddleTime();

  /**
   * Gets a duration to wait before cycling to another match.
   *
   * @return A duration, if zero then cycles immediately, if negative does not auto-cycle.
   */
  Duration getCycleTime();

  /**
   * Gets a duration to wait before the server should restart.
   *
   * @return A duration, disabled if non-positive.
   */
  Duration getUptimeLimit();

  /**
   * Gets the maximum number of matches before the server should restart.
   *
   * @return A maximum number of matches, disabled if non-positive.
   */
  long getMatchLimit();

  /**
   * Gets the minimum number of players for a match to start.
   *
   * @return A minimum number of players, disabled if non-positive.
   */
  long getMinimumPlayers();

  /**
   * Gets whether observers can join a match after it has started.
   *
   * <p>Blitz matches are exempt from this rule, you can never join during a blitz match.
   *
   * @return If observers can join during a match.
   */
  boolean canAnytimeJoin();

  /**
   * Gets whether map limits on team sizes should be enforced.
   *
   * @return If size limits are enforced.
   */
  boolean shouldLimitJoin();

  /**
   * Gets whether teams should, on a best-case basis, try to be rebalanced.
   *
   * @return If team re-balancing is enabled.
   */
  boolean shouldBalanceJoin();

  /**
   * Gets whether teams are required to be balanced, using a queue system.
   *
   * @return If teams are required to be balanced.
   */
  boolean shouldQueueJoin();

  /**
   * Gets whether non-premium players can be kicked to make room for a premium player.
   *
   * @return If priority kick is enabled.
   */
  boolean canPriorityKick();

  /**
   * Gets whether proximity metrics are visible to players.
   *
   * @return If proximity is visible.
   */
  boolean showProximity();

  /**
   * Gets whether the side bar is rendered.
   *
   * @return If the side bar is rendered.
   */
  boolean showSideBar();

  /**
   * Gets a header for the side bar.
   *
   * @return The side bar header, or null to defer to the map's title.
   */
  @Nullable
  Component getMatchHeader();

  /**
   * Gets a footer for the side bar.
   *
   * @return The side bar footer, or null for none.
   */
  @Nullable
  Component getMatchFooter();

  /**
   * Gets whether the tab list is rendered.
   *
   * @return If the tab list is rendered.
   */
  boolean showTabList();

  /**
   * Gets whether the tab list is should show real ping.
   *
   * @return If the tab list should show real ping.
   */
  boolean showTabListPing();

  /**
   * Gets whether observers are shown to participants in the tab list.
   *
   * @return If observers should be visible to participants.
   */
  boolean canParticipantsSeeObservers();

  /**
   * Gets whether to show fireworks when an objective is completed or when the match ends.
   *
   * @return If fireworks should be shown.
   */
  boolean showFireworks();

  /**
   * Gets a format to override the server's "message of the day."
   *
   * <p>{0} = The existing MoTD.
   *
   * <p>{1} = Name of the map currently playing.
   *
   * <p>{2} = A color code representing the current match state.
   *
   * @return A motd format, or null to leave unmodified.
   */
  @Nullable
  String getMotd();

  /**
   * Gets whether wool in capture the wool maps are auto refilled.
   *
   * @return If wool auto refill is enabled.
   */
  boolean shouldRefillWool();

  /**
   * Gets a group of players, used for prefixes and player sorting.
   *
   * @return A list of groups.
   */
  List<? extends Group> getGroups();

  /** A group of players with a common permission set and optional prefix. */
  interface Group {

    /**
     * Gets a unique id for the group.
     *
     * @return A group id.
     */
    String getId();

    /**
     * Gets the prefix to show next to each player's name.
     *
     * @return A chat prefix, or null for none.
     */
    @Nullable
    String getPrefix();

    /**
     * Gets the permission node required to be included in this group.
     *
     * @return A permission node, or "op" for server operator.
     */
    Permission getPermission();

    /**
     * Gets a map of permissions applied when players are in observers.
     *
     * @return A permissions map.
     */
    Permission getObserverPermission();

    /**
     * Gets a map of permissions applied when players are participating.
     *
     * @return A permissions map.
     */
    Permission getParticipantPermission();
  }

  /**
   * Gets whether "community mode" should be installed if not present.
   *
   * <p>Includes features such as /report, /warn, /freeze, and more.
   *
   * @return If community mode is enabled.
   */
  boolean isCommunityMode();

  /**
   * Gets experimental configuration settings that are not yet stable.
   *
   * @return A map of experimental settings.
   */
  Map<String, Object> getExperiments();
}

package tc.oc.pgm.api;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.Nullable;
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
  Path getMapPoolFile();

  /**
   * Gets a path to the includes directory.
   *
   * @return A path to the includes directory, or null for none.
   */
  @Nullable
  Path getIncludesDirectory();

  /** @return If unused XML tags should be reported or ignored */
  boolean showUnusedXml();

  /** @return Should setting dev phase maps be restricted to Devs */
  boolean enforceDevPhase();

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
   * Gets a duration to wait before restarting the server.
   *
   * @return A duration.
   */
  Duration getRestartTime();

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
   * Gets the left side text for the Tablist.
   *
   * @return The left side component, or null for none.
   */
  @Nullable
  Component getLeftTablistText();

  /**
   * Gets the right side text for the Tablist.
   *
   * @return The right side component, or null for none.
   */
  @Nullable
  Component getRightTablistText();

  /**
   * Gets whether the tab list is rendered.
   *
   * @return If the tab list is rendered.
   */
  boolean showTabList();

  /**
   * Gets whether the tab list should be resized to 4 rows for 1.7 players.
   *
   * @return If the tab list will be resized.
   */
  boolean resizeTabList();

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
   * Whether the wool flag beams created for older versions (pre-1.8) should be shown to all
   * players.
   *
   * @return If the wool flag beams should be shown to players >=1.8
   */
  boolean useLegacyFlagBeams();

  /**
   * Gets whether to show a more verbose representation of the match stats at the end of each match
   *
   * @return If verbose stats at the end of the match is enabled
   */
  boolean showVerboseStats();

  /** @return How many ticks should wait until showing stats */
  Duration showStatsAfter();

  /** @return If stats on match end should shown high scores */
  boolean showBestStats();

  /** @return If stats on match end should show your own stats */
  boolean showOwnStats();

  /** @return The slot where the verbose item will be placed */
  int getVerboseItemSlot();

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
   * Gets at which score players should be no longer allowed to keep playing TDM
   *
   * @return The minimum score they must hold
   */
  int getGriefScore();

  /**
   * Gets the percentage of damage needed on a player to get an assist
   *
   * @return The percentage of damage required
   */
  float getAssistPercent();

  /**
   * Gets how long to penalize players for certain actions
   *
   * @return time to make them sit out for
   */
  Duration getTimePenalty(TimePenalty penalty);

  enum TimePenalty {
    FFA_FULL_REJOIN,
    STACKED,
    FULL_REJOIN,
    REJOIN_MULTIPLIER,
    REJOIN_MAX,
    SWITCH
  }

  /**
   * Gets if extra votes are allowed based on the "pgm.vote.extra.#" permission.
   *
   * @return {@code true} if extra votes are enabled, {@code false} otherwise.
   */
  boolean allowExtraVotes();

  /**
   * Gets the maximum number of extra votes a player can use.
   *
   * @return The maximum number of extra votes allowed per player.
   */
  int getMaxExtraVotes();

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
     * Gets a flair object which holds all prefix/suffix related data
     *
     * @return A {@link Flair} for this group
     */
    Flair getFlair();

    /**
     * Gets the prefix to show next to each player's name.
     *
     * @return A chat prefix, or null for none.
     */
    @Nullable
    default String getPrefix() {
      return getFlair().getPrefix();
    }

    /**
     * Gets the suffix to show next to each player's name.
     *
     * @return A chat suffix, or null for none.
     */
    @Nullable
    default String getSuffix() {
      return getFlair().getSuffix();
    }

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

  interface Flair {

    String getPrefix();

    String getSuffix();

    String getDescription();

    String getDisplayName();

    String getClickLink();

    Component getPrefixOverride();

    Component getSuffixOverride();

    default Component getComponent(boolean prefix) {
      if (prefix ? getPrefixOverride() != null : getSuffixOverride() != null) {
        return prefix ? getPrefixOverride() : getSuffixOverride();
      }
      TextComponent.Builder hover = text();
      boolean addNewline = false;
      if (getDisplayName() != null && !getDisplayName().isEmpty()) {
        addNewline = true;
        hover.append(text(getDisplayName()));
      }
      if (getDescription() != null && !getDescription().isEmpty()) {
        if (addNewline) hover.append(newline());
        addNewline = true;
        hover.append(text(getDescription()));
      }

      if (getClickLink() != null && !getClickLink().isEmpty()) {
        if (addNewline) hover.append(newline());

        Component clickLink = translatable(
            "chat.clickLink",
            NamedTextColor.DARK_AQUA,
            text(getClickLink(), NamedTextColor.AQUA, TextDecoration.UNDERLINED));
        hover.append(clickLink);
      }

      TextComponent.Builder component = text()
          .append(text(prefix ? getPrefix() : getSuffix()))
          .hoverEvent(showText(hover.build()));

      if (getClickLink() != null && !getClickLink().isEmpty()) {
        component.clickEvent(openUrl(getClickLink()));
      }

      return component.build();
    }
  }

  /**
   * Gets whether a simple vanish manager should be installed.
   *
   * <p>Allows for basic usage of /vanish. If you wish to allow third-party plugins to hook-in
   * disable this
   *
   * @return If vanish is enabled.
   */
  boolean isVanishEnabled();

  /**
   * Gets experimental configuration settings that are not yet stable.
   *
   * @return A map of experimental settings.
   */
  Map<String, Object> getExperiments();

  default boolean getExperimentAsBool(String key, boolean def) {
    Object exp = getExperiments().getOrDefault(key, def);
    return exp instanceof Boolean ? (Boolean) exp : exp.toString().equals("true");
  }
}

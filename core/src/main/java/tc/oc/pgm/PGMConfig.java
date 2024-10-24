package tc.oc.pgm;

import static tc.oc.pgm.util.text.TextParser.parseBoolean;
import static tc.oc.pgm.util.text.TextParser.parseComponent;
import static tc.oc.pgm.util.text.TextParser.parseComponentLegacy;
import static tc.oc.pgm.util.text.TextParser.parseDuration;
import static tc.oc.pgm.util.text.TextParser.parseEnum;
import static tc.oc.pgm.util.text.TextParser.parseFloat;
import static tc.oc.pgm.util.text.TextParser.parseInteger;
import static tc.oc.pgm.util.text.TextParser.parseLogLevel;
import static tc.oc.pgm.util.text.TextParser.parseUri;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.map.source.GitMapSourceFactory;
import tc.oc.pgm.map.source.PathMapSourceFactory;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextException;

public final class PGMConfig implements Config {

  // log-level
  private final Level logLevel;

  // database-uri
  private final String databaseUri;
  private final int databaseMaxConnections;

  // motd
  private final String motd;

  // map.*
  private final List<MapSourceFactory> mapSourceFactories;
  private final Path mapPoolFile;
  private final Path includesDirectory;
  private final boolean showUnusedXml;
  private final boolean enforceDevPhase;

  // countdown.*
  private final Duration startTime;
  private final Duration huddleTime;
  private final Duration cycleTime;
  private final Duration restartTime;

  // restart.*
  private final Duration uptimeLimit;
  private final long matchLimit;

  // gameplay.*
  private final boolean woolRefill;
  private final int griefScore;
  private final float assistPercent;
  private final Map<TimePenalty, Duration> timePenalties;

  // votes.*
  private final boolean allowExtraVotes;
  private final int maxExtraVotes;

  // join.*
  private final long minPlayers;
  private final boolean limitJoin;
  private final boolean priorityKick;
  private final boolean balanceJoin;
  private final boolean queueJoin;
  private final boolean anytimeJoin;
  private final boolean flagBeams;

  // ui.*
  private final boolean showSideBar;
  private final boolean showTabList;
  private final boolean resizeTabList;
  private final boolean showTabListPing;
  private final boolean showProximity;
  private final boolean showFireworks;
  private final boolean participantsSeeObservers;
  private final boolean verboseStats;
  private final Duration statsShowAfter;
  private final boolean statsShowBest;
  private final boolean statsShowOwn;
  private final int verboseItemSlot;

  // sidebar.*
  private final Component header;
  private final Component footer;

  // tablist.*
  private final Component rightTablistText;
  private final Component leftTablistText;

  // community.*
  private final boolean vanish;

  // groups.*
  private final List<Group> groups;

  // experiments.*
  private final Map<String, Object> experiments;

  PGMConfig(FileConfiguration config, File dataFolder) throws TextException {
    handleLegacyConfig(config, dataFolder);

    this.logLevel = parseLogLevel(config.getString("log-level", "info"));

    final String databaseUri = config.getString("database-uri");
    this.databaseUri = databaseUri == null || databaseUri.isEmpty()
        ? new File(dataFolder, "pgm.db")
            .getAbsoluteFile()
            .toURI()
            .toString()
            .replaceFirst("^file", "sqlite")
        : databaseUri;
    this.databaseMaxConnections = this.databaseUri.startsWith("sqlite:")
        ? 1 // SQLite is single threaded by nature
        : Math.min(
            config.getInt("database-max-connections", 5),
            Runtime.getRuntime().availableProcessors());

    final String motd = config.getString("motd");
    this.motd = motd == null || motd.isEmpty() ? null : parseComponentLegacy(motd);

    this.mapSourceFactories = new LinkedList<>();

    final TreeSet<String> folders = new TreeSet<>(config.getStringList("map.folders"));
    final List<Map<?, ?>> repositories = new LinkedList<>(config.getMapList("map.repositories"));

    for (String uri : config.getStringList("map.repositories")) {
      repositories.add(ImmutableMap.of("uri", uri));
    }

    for (Map<?, ?> repository : repositories) {
      registerRemoteMapSource(mapSourceFactories, repository);
    }

    for (String folder : folders) {
      this.mapSourceFactories.add(new PathMapSourceFactory(Paths.get(folder)));
    }

    this.mapPoolFile = getPath(dataFolder.toPath(), config.getString("map.pools"));
    this.includesDirectory = getPath(dataFolder.toPath(), config.getString("map.includes"));
    this.showUnusedXml = parseBoolean(config.getString("map.show-unused-xml", "true"));
    this.enforceDevPhase = parseBoolean(config.getString("map.enforce-dev-phase", "false"));

    this.startTime = parseDuration(config.getString("countdown.start", "30s"));
    this.huddleTime = parseDuration(config.getString("countdown.huddle", "0s"));
    this.cycleTime = parseDuration(config.getString("countdown.cycle", "30s"));
    this.restartTime = parseDuration(config.getString("countdown.restart", "30s"));

    this.uptimeLimit = parseDuration(config.getString("restart.uptime", "1d"));
    this.matchLimit = parseInteger(config.getString("restart.match-limit", "30"));

    this.woolRefill = parseBoolean(config.getString("gameplay.refill-wool", "true"));
    this.griefScore =
        parseInteger(config.getString("gameplay.grief-score", "-10"), Range.atMost(0));
    this.assistPercent =
        parseFloat(config.getString("gameplay.assist-percent", "0.3"), Range.openClosed(0f, 1f));
    this.timePenalties = new EnumMap<>(TimePenalty.class);
    for (TimePenalty penalty : TimePenalty.values()) {
      String key = penalty.name().toLowerCase(Locale.ROOT).replace('_', '-');
      timePenalties.put(
          penalty, parseDuration(config.getString("gameplay.time-penalties." + key, "0")));
    }

    this.allowExtraVotes = parseBoolean(config.getString("votes.allow-extra-votes", "true"));
    this.maxExtraVotes = parseInteger(config.getString("votes.max-extra-votes", "5"));

    this.minPlayers = parseInteger(config.getString("join.min-players", "1"));
    this.limitJoin = parseBoolean(config.getString("join.limit", "true"));
    this.priorityKick = parseBoolean(config.getString("join.priority-kick", "true"));
    this.balanceJoin = parseBoolean(config.getString("join.balance", "true"));
    this.queueJoin = parseBoolean(config.getString("join.queue", "false"));
    this.anytimeJoin = parseBoolean(config.getString("join.anytime", "true"));

    this.showProximity = parseBoolean(config.getString("ui.proximity", "false"));
    this.showSideBar = parseBoolean(config.getString("ui.sidebar", "true"));
    this.showTabList = parseBoolean(config.getString("ui.tablist", "true"));
    this.resizeTabList = parseBoolean(config.getString("ui.tablist-resize", "false"));
    this.showTabListPing = parseBoolean(config.getString("ui.ping", "true"));
    this.participantsSeeObservers =
        parseBoolean(config.getString("ui.participants-see-observers", "true"));
    this.showFireworks = parseBoolean(config.getString("ui.fireworks", "true"));
    this.flagBeams = parseBoolean(config.getString("ui.flag-beams", "false"));

    this.verboseStats = parseBoolean(config.getString("stats.verbose", "true"));
    this.statsShowAfter = parseDuration(config.getString("stats.show-after", "6s"));
    this.statsShowBest = parseBoolean(config.getString("stats.show-best", "true"));
    this.statsShowOwn = parseBoolean(config.getString("stats.show-own", "true"));
    this.verboseItemSlot = parseInteger(config.getString("stats.item-slot", "7"));

    final String header = config.getString("sidebar.header");
    this.header = header == null || header.isEmpty() ? null : parseComponent(header);
    final String footer = config.getString("sidebar.footer");
    this.footer = footer == null || footer.isEmpty() ? null : parseComponent(footer);
    final String leftText = config.getString("tablist.left");
    this.leftTablistText = leftText == null || leftText.isEmpty() ? null : parseComponent(leftText);
    final String rightText = config.getString("tablist.right");
    this.rightTablistText =
        rightText == null || rightText.isEmpty() ? null : parseComponent(rightText);

    this.vanish = parseBoolean(config.getString("vanish", "true"));

    final ConfigurationSection section = config.getConfigurationSection("groups");
    this.groups = new ArrayList<>();
    if (section != null) {
      for (String key : section.getKeys(false)) {
        groups.add(new Group(section.getConfigurationSection(key)));
      }
    }

    final ConfigurationSection experiments = config.getConfigurationSection("experiments");
    this.experiments = experiments == null ? ImmutableMap.of() : experiments.getValues(false);
  }

  private Path getPath(Path base, String dir) {
    if (dir == null || dir.isEmpty()) return null;
    Path path = Paths.get(dir);
    return path.isAbsolute() ? path : base.resolve(path);
  }

  public static final Map<?, ?> DEFAULT_REMOTE_REPO =
      ImmutableMap.of("uri", "https://github.com/PGMDev/Maps", "path", "default-maps");

  public static void registerRemoteMapSource(
      List<MapSourceFactory> mapSources, Map<?, ?> repository) {
    final URI uri = parseUri(String.valueOf(repository.get("uri")));

    String branch = String.valueOf(repository.get("branch"));
    if (branch.isEmpty() || branch.equals("null")) {
      branch = null;
    }

    String path = String.valueOf(repository.get("path"));
    if (path.isEmpty() || path.equals("null")) {
      String normalizedPath = Normalizer.normalize(
              uri.getHost() + uri.getPath(), Normalizer.Form.NFD)
          .replaceAll("[^A-Za-z0-9_]", "-")
          .toLowerCase(Locale.ROOT);
      path = "maps" + File.separator + normalizedPath;
    }

    Path base = Paths.get(path).toAbsolutePath();

    // Set up a path filter, if needed
    List<Path> children = null;
    final Object subFolders = repository.get("folders");
    if (subFolders instanceof List) {
      children = ((List<?>) subFolders)
          .stream().map(Object::toString).map(Paths::get).collect(Collectors.toList());
    }

    mapSources.add(new GitMapSourceFactory(base, children, uri, branch));
  }

  // TODO: Can be removed after 1.0 release
  private static void handleLegacyConfig(FileConfiguration config, File dataFolder) {
    // v0.9 uses map.folders instead of map.sources
    if (config.get("map.sources") != null) {
      renameKey(config, "map.sources", "map.folders");

      if (config.getStringList("map.folders").contains("default")) {
        config.set("map.repositories", ImmutableList.of(DEFAULT_REMOTE_REPO));
      }

      try {
        config.save(new File(dataFolder, "config.yml"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // A somewhat hacky way of determining if this config need to be converted
    if (config.get("arrow-removal") == null) return;

    if (config.getBoolean("map-pools.enabled", true)) {
      renameKey(config, "map-pools.path", "map.pools");
    } else {
      config.set("map.pools", "");
    }
    config.set("map-pools", null);

    if (!config.getBoolean("restart.enabled", true)) {
      config.set("restart.uptime", -1);
      config.set("restart.match-limit", -1);
    }
    config.set("restart.memory", null);
    config.set("restart.enabled", null);

    renameKey(config, "start.countdown", "countdown.start");
    renameKey(config, "start.huddle", "countdown.huddle");
    if (!config.getBoolean("start.auto", true)) {
      config.set("countdown.start", -1);
    }
    config.set("start", null);

    renameKey(config, "cycle.countdown", "countdown.cycle");
    if (!config.getBoolean("cycle.match-empty.enabled", true)) {
      config.set("join.min-players", 0);
    }
    config.set("cycle", null);

    renameKey(config, "join.mid-match", "join.anytime");
    if (config.getBoolean("join.commit-players", false)
        || config.getBoolean("teams.require-even")) {
      config.set("join.queue", true);
    }
    config.set("join.commit-players", null);
    renameKey(config, "join.capacity.enabled", "join.limit");
    config.set("join.capacity", null);

    renameKey(config, "teams.try-balance", "join.balance");
    renameKey(config, "minimum-players", "join.min-players");

    config.set("teams", null);
    config.set("broadcast", null);
    config.set("arrow-removal", null);
    config.set("fishing", null);

    renameKey(config, "scoreboard.show-proximity", "ui.proximity");
    config.set("scoreboard", null);

    renameKey(config, "wool.auto-refill", "gameplay.refill-wool");
    config.set("wool", null);

    renameKey(config, "player-list.enabled", "ui.tablist");
    config.set("player-list", null);

    if (config.getBoolean("motd.enabled", true)) {
      final String format = config
          .getString("motd.format", "")
          .replace("{state.color}", "{2}")
          .replace("{map.name}", "{1}")
          .replace("{map.version}", "1.0.0")
          .replace("{state.name}", "Idle")
          .replace("{state.name-lower}", "idle");
      config.set("motd", format);
    } else {
      config.set("motd", "");
    }

    // priority:id:prefix
    final List<String> groups = new ArrayList<>();

    final ConfigurationSection section = config.getConfigurationSection("groups");
    if (section != null) {
      for (String key : section.getKeys(false)) {
        final String id = section.getBoolean(key + ".op", false) ? "op" : key;
        final int priority = section.getInt(key + ".priority", Integer.MAX_VALUE);
        String prefix = null;
        try {
          prefix = "&"
              + parseEnum(section.getString(key + ".prefix.color"), ChatColor.class)
                  .getChar()
              + section.getString(key + ".prefix.symbol");
        } catch (TextException | NullPointerException e) {
          // No-op
        }

        groups.add(String.format("%s|%s|%s", priority, id, prefix == null ? "" : prefix));
      }
    }

    // Will be sorted based on priority, in ascending order
    Collections.sort(groups, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        try {
          return Integer.parseInt(o1.split("\\|")[0]) - Integer.parseInt(o2.split("\\|")[0]);
        } catch (Throwable t) {
          return 0;
        }
      }
    });

    final Map<String, Map<String, Object>> data = new LinkedHashMap<>();
    for (String group : groups) {
      String[] parts = group.split("\\|", 3);

      data.put(
          parts[1], parts[2].isEmpty() ? ImmutableMap.of() : ImmutableMap.of("prefix", parts[2]));
    }

    config.set("groups", null);
    config.createSection("groups", data);

    config.set(
        "groups.default.permissions",
        ImmutableList.of(
            "-bukkit.command.kill",
            "-bukkit.command.me",
            "-bukkit.command.tell",
            "-worldedit.navigation.ceiling",
            "-worldedit.navigation.up",
            "-worldedit.calc",
            "-commandbook.pong",
            "-commandbook.speed.flight",
            "-commandbook.speed.walk"));
    config.set(
        "groups.default.observer-permissions",
        ImmutableList.of("+worldedit.navigation.*", "+commandbook.teleport"));
    config.set(
        "groups.default.participant-permissions",
        ImmutableList.of(
            "-worldedit.navigation.thru.tool",
            "-worldedit.navigation.jumpto.tool",
            "-commandbook.teleport"));

    renameKey(config, "sidebar.top", "sidebar.header");
    renameKey(config, "sidebar.bottom", "sidebar.footer");
    config.set("sidebar.overwrite", null);
    config.set("ui.sidebar", true);
    config.set("ui.participants-see-observers", true);

    renameKey(config, "moderation", "community");
    config.set("community.enabled", true);

    renameKey(config, "fireworks.post-match", "ui.fireworks");
    config.set("fireworks", null);

    try {
      config.save(new File(dataFolder, "config.yml"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void renameKey(FileConfiguration config, String from, String to) {
    final Object value = config.get(from);
    if (value == null) return;

    config.set(to, value);
    config.set(from, null);
  }

  @Override
  public Level getLogLevel() {
    return logLevel;
  }

  @Override
  public String getDatabaseUri() {
    return databaseUri;
  }

  @Override
  public int getDatabaseMaxConnections() {
    return databaseMaxConnections;
  }

  @Override
  public List<? extends MapSourceFactory> getMapSourceFactories() {
    return mapSourceFactories;
  }

  @Override
  public Path getMapPoolFile() {
    return mapPoolFile;
  }

  @Override
  public @Nullable Path getIncludesDirectory() {
    return includesDirectory;
  }

  @Override
  public boolean showUnusedXml() {
    return showUnusedXml;
  }

  @Override
  public boolean enforceDevPhase() {
    return enforceDevPhase;
  }

  @Override
  public Duration getStartTime() {
    return startTime;
  }

  @Override
  public Duration getHuddleTime() {
    return huddleTime;
  }

  @Override
  public Duration getCycleTime() {
    return cycleTime;
  }

  @Override
  public Duration getRestartTime() {
    return restartTime;
  }

  @Override
  public Duration getUptimeLimit() {
    return uptimeLimit;
  }

  @Override
  public long getMatchLimit() {
    return matchLimit;
  }

  @Override
  public long getMinimumPlayers() {
    return minPlayers;
  }

  @Override
  public boolean shouldLimitJoin() {
    return limitJoin;
  }

  @Override
  public boolean canPriorityKick() {
    return priorityKick;
  }

  @Override
  public boolean shouldBalanceJoin() {
    return balanceJoin;
  }

  @Override
  public boolean shouldQueueJoin() {
    return queueJoin;
  }

  @Override
  public boolean showProximity() {
    return showProximity;
  }

  @Override
  public boolean shouldRefillWool() {
    return woolRefill;
  }

  @Override
  public int getGriefScore() {
    return griefScore;
  }

  @Override
  public float getAssistPercent() {
    return assistPercent;
  }

  @Override
  public Duration getTimePenalty(TimePenalty penalty) {
    return timePenalties.get(penalty);
  }

  @Override
  public boolean showSideBar() {
    return showSideBar;
  }

  @Override
  public Component getMatchHeader() {
    return header;
  }

  @Override
  public Component getMatchFooter() {
    return footer;
  }

  @Override
  public Component getLeftTablistText() {
    return leftTablistText;
  }

  @Override
  public Component getRightTablistText() {
    return rightTablistText;
  }

  @Override
  public boolean showTabList() {
    return showTabList;
  }

  @Override
  public boolean resizeTabList() {
    return resizeTabList;
  }

  @Override
  public boolean showTabListPing() {
    return showTabListPing;
  }

  @Override
  public boolean canParticipantsSeeObservers() {
    return participantsSeeObservers;
  }

  @Override
  public boolean canAnytimeJoin() {
    return anytimeJoin;
  }

  @Override
  public boolean showFireworks() {
    return showFireworks;
  }

  @Override
  public boolean useLegacyFlagBeams() {
    return flagBeams;
  }

  public boolean showVerboseStats() {
    return verboseStats;
  }

  @Override
  public Duration showStatsAfter() {
    return statsShowAfter;
  }

  @Override
  public boolean showBestStats() {
    return statsShowBest;
  }

  @Override
  public boolean showOwnStats() {
    return statsShowOwn;
  }

  @Override
  public int getVerboseItemSlot() {
    return verboseItemSlot;
  }

  @Override
  public String getMotd() {
    return motd;
  }

  @Override
  public boolean allowExtraVotes() {
    return allowExtraVotes;
  }

  @Override
  public int getMaxExtraVotes() {
    return maxExtraVotes;
  }

  @Override
  public List<Group> getGroups() {
    return groups;
  }

  @Override
  public boolean isVanishEnabled() {
    return vanish;
  }

  @Override
  public Map<String, Object> getExperiments() {
    return experiments;
  }

  private static class Group implements Config.Group {
    private final String id;
    private final Flair flair;
    private final Permission permission;
    private final Permission observerPermission;
    private final Permission participantPermission;

    public Group(ConfigurationSection config) throws TextException {
      this.id = config.getName();
      this.flair = new Flair(config);
      final PermissionDefault def = id.equalsIgnoreCase("op")
          ? PermissionDefault.OP
          : id.equalsIgnoreCase("default") ? PermissionDefault.TRUE : PermissionDefault.FALSE;
      this.permission = getPermission(config, id, def, "permissions");
      this.observerPermission =
          getPermission(config, id, PermissionDefault.FALSE, "observer-permissions");
      this.participantPermission =
          getPermission(config, id, PermissionDefault.FALSE, "participant-permissions");
    }

    private static Permission getPermission(
        ConfigurationSection config, String id, PermissionDefault def, String realm) {
      final Map<String, Boolean> permissions = new HashMap<>();

      for (String permission : config.getStringList(realm)) {
        if (permission.startsWith("-")) {
          permissions.put(permission.substring(1), false);
        } else if (permission.startsWith("+")) {
          permissions.put(permission.substring(1), true);
        } else {
          permissions.put(permission, true);
        }
      }

      final String node = Permissions.GROUP
          + "."
          + id
          + (realm.contains("-") ? "-" + realm.substring(0, realm.indexOf('-')) : "");

      return Permissions.register(new Permission(node, def, permissions));
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public Permission getPermission() {
      return permission;
    }

    @Override
    public Permission getObserverPermission() {
      return observerPermission;
    }

    @Override
    public Permission getParticipantPermission() {
      return participantPermission;
    }

    @Override
    public Flair getFlair() {
      return flair;
    }
  }

  private static class Flair implements Config.Flair {

    private String prefix;
    private String suffix;
    private String displayName;
    private String description;
    private String clickLink;
    private Component prefixOverride;
    private Component suffixOverride;

    public Flair(ConfigurationSection config) {
      final String prefix = config.getString("prefix");
      this.prefix = prefix == null ? null : parseComponentLegacy(prefix);

      final String suffix = config.getString("suffix");
      this.suffix = suffix == null ? null : parseComponentLegacy(suffix);

      final String name = config.getString("display-name");
      this.displayName = name == null ? null : parseComponentLegacy(name);

      final String desc = config.getString("description");
      this.description = desc == null ? null : parseComponentLegacy(desc);

      final String link = config.getString("click-link");
      this.clickLink = link == null ? null : parseComponentLegacy(link);

      final String prefixComp = config.getString("prefix-component");
      this.prefixOverride =
          prefixComp == null || prefixComp.isEmpty() ? null : parseComponent(prefixComp);

      final String suffixComp = config.getString("suffix-component");
      this.suffixOverride =
          suffixComp == null || suffixComp.isEmpty() ? null : parseComponent(suffixComp);
    }

    @Override
    public String getPrefix() {
      return prefix;
    }

    @Override
    public String getSuffix() {
      return suffix;
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public String getDisplayName() {
      return displayName;
    }

    @Override
    public String getClickLink() {
      return clickLink;
    }

    @Override
    public Component getPrefixOverride() {
      return prefixOverride;
    }

    @Override
    public Component getSuffixOverride() {
      return suffixOverride;
    }
  }

  @Deprecated
  public static class Moderation {

    public static boolean isRuleLinkVisible() {
      return getRulesLink().length() > 0;
    }

    public static String getRulesLink() {
      return BukkitUtils.colorize(PGM.get().getConfig().getString("community.rules-link", ""));
    }

    public static String getServerName() {
      return BukkitUtils.colorize(PGM.get().getConfig().getString("community.server-name", ""));
    }

    public static String getAppealMessage() {
      return BukkitUtils.colorize(PGM.get().getConfig().getString("community.appeal-msg", ""));
    }

    public static boolean isAppealVisible() {
      return getAppealMessage().length() > 0;
    }
  }
}

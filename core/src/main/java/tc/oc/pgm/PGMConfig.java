package tc.oc.pgm;

import static tc.oc.pgm.util.text.TextParser.parseBoolean;
import static tc.oc.pgm.util.text.TextParser.parseComponent;
import static tc.oc.pgm.util.text.TextParser.parseComponentLegacy;
import static tc.oc.pgm.util.text.TextParser.parseDuration;
import static tc.oc.pgm.util.text.TextParser.parseEnum;
import static tc.oc.pgm.util.text.TextParser.parseInteger;
import static tc.oc.pgm.util.text.TextParser.parseLogLevel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextException;

public final class PGMConfig implements Config {

  // log-level
  private final Level logLevel;

  // database-uri
  private final String databaseUri;

  // motd
  private final String motd;

  // map.*
  private final List<String> mapSources;
  private final String mapPoolFile;

  // countdown.*
  private final Duration startTime;
  private final Duration huddleTime;
  private final Duration cycleTime;

  // restart.*
  private final Duration uptimeLimit;
  private final long matchLimit;

  // gameplay.*
  private final boolean woolRefill;

  // join.*
  private final long minPlayers;
  private final boolean limitJoin;
  private final boolean priorityKick;
  private final boolean balanceJoin;
  private final boolean queueJoin;
  private final boolean anytimeJoin;

  // ui.*
  private final boolean showSideBar;
  private final boolean showTabList;
  private final boolean showProximity;
  private final boolean showFireworks;
  private final boolean participantsSeeObservers;

  // sidebar.*
  private final Component header;
  private final Component footer;

  // community.*
  private final boolean communityMode;

  // groups.*
  private final List<Group> groups;

  // experiments.*
  private final Map<String, Object> experiments;

  PGMConfig(FileConfiguration config, File dataFolder) throws TextException {
    handleLegacyConfig(config, dataFolder);

    this.logLevel = parseLogLevel(config.getString("log-level", "info"));

    this.databaseUri = config.getString("database-uri");

    final String motd = config.getString("motd");
    this.motd = motd == null || motd.isEmpty() ? null : parseComponentLegacy(motd);

    this.mapSources = config.getStringList("map.sources");
    final String mapPoolFile = config.getString("map.pools");
    this.mapPoolFile =
        mapPoolFile == null || mapPoolFile.isEmpty()
            ? null
            : new File(dataFolder, mapPoolFile).getAbsolutePath();

    this.startTime = parseDuration(config.getString("countdown.start", "30s"));
    this.huddleTime = parseDuration(config.getString("countdown.huddle", "0s"));
    this.cycleTime = parseDuration(config.getString("countdown.cycle", "30s"));

    this.uptimeLimit = parseDuration(config.getString("restart.uptime", "1d"));
    this.matchLimit = parseInteger(config.getString("restart.match-limit", "30"));

    this.woolRefill = parseBoolean(config.getString("gameplay.refill-wool", "true"));

    this.minPlayers = parseInteger(config.getString("join.min-players", "1"));
    this.limitJoin = parseBoolean(config.getString("join.limit", "true"));
    this.priorityKick = parseBoolean(config.getString("join.priority-kick", "true"));
    this.balanceJoin = parseBoolean(config.getString("join.balance", "true"));
    this.queueJoin = parseBoolean(config.getString("join.queue", "false"));
    this.anytimeJoin = parseBoolean(config.getString("join.anytime", "true"));

    this.showProximity = parseBoolean(config.getString("ui.proximity", "false"));
    this.showSideBar = parseBoolean(config.getString("ui.sidebar", "true"));
    this.showTabList = parseBoolean(config.getString("ui.tablist", "true"));
    this.participantsSeeObservers =
        parseBoolean(config.getString("ui.participants-see-observers", "true"));
    this.showFireworks = parseBoolean(config.getString("ui.fireworks", "true"));
    final String header = config.getString("sidebar.header");
    this.header = header == null || header.isEmpty() ? null : parseComponent(header);
    final String footer = config.getString("sidebar.footer");
    this.footer = footer == null || footer.isEmpty() ? null : parseComponent(footer);

    this.communityMode = parseBoolean(config.getString("community.enabled", "true"));

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

  // TODO: Can be removed after 1.0 release
  private static void handleLegacyConfig(FileConfiguration config, File dataFolder) {
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
      final String format =
          config
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
          prefix =
              "&"
                  + parseEnum(section.getString(key + ".prefix.color"), ChatColor.class).getChar()
                  + section.getString(key + ".prefix.symbol");
        } catch (TextException | NullPointerException e) {
          // No-op
        }

        groups.add(String.format("%s|%s|%s", priority, id, prefix == null ? "" : prefix));
      }
    }

    // Will be sorted based on priority, in ascending order
    Collections.sort(
        groups,
        new Comparator<String>() {
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
  public List<String> getMapSources() {
    return mapSources;
  }

  @Override
  public String getMapPoolFile() {
    return mapPoolFile;
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
  public boolean showTabList() {
    return showTabList;
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
  public String getMotd() {
    return motd;
  }

  @Override
  public List<Group> getGroups() {
    return groups;
  }

  @Override
  public boolean isCommunityMode() {
    return communityMode;
  }

  @Override
  public Map<String, Object> getExperiments() {
    return experiments;
  }

  private static class Group implements Config.Group {
    private final String id;
    private final String prefix;
    private final Permission permission;
    private final Permission observerPermission;
    private final Permission participantPermission;

    public Group(ConfigurationSection config) throws TextException {
      this.id = config.getName();
      final String prefix = config.getString("prefix");
      this.prefix = prefix == null ? null : parseComponentLegacy(prefix);
      final PermissionDefault def =
          id.equalsIgnoreCase("op")
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

      final String node =
          Permissions.GROUP
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
    public String getPrefix() {
      return prefix;
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

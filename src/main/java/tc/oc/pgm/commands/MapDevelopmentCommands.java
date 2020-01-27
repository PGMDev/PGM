package tc.oc.pgm.commands;

import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.param.Switch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.FileUtils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.Feature;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterDefinition;
import tc.oc.pgm.map.MapNotFoundException;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.map.PGMMap.MapLogRecord;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.world.NMSHacks;

public class MapDevelopmentCommands {

  @Command(
          name = "clearerrors",
          aliases = {"clearxmlerrors"},
          desc = "Clears XML errors")
  public static void clearErrorsCommand(CommandSender sender) {
    getErrors().clear();
    sender.sendMessage(
        ChatColor.GREEN
            + AllTranslations.get().translate("command.development.clearErrors.success", sender));
  }

  @Command(
          name = "errors",
          aliases = {"xmlerrors"},
          desc = "Reads back XML errors",
          descFooter = "[map] <page>")
  public static void errorsCommand(
      CommandSender sender, Audience audience, PGMMap map, /*@Default("1")TODO ADD DEFAULT*/ int page)
  {
    Multimap<PGMMap, PGMMap.MapLogRecord> errors = getErrors();
    Multimap<PGMMap, PGMMap.MapLogRecord> filtered = ArrayListMultimap.create();

    if (map != null) {
      filtered.putAll(map, errors.get(map));
      errors = filtered;
    }

    new PrettyPaginatedResult<Entry<PGMMap, MapLogRecord>>(
        map == null ? "Errors" : map.getName() + " Errors") {
      @Override
      public String format(Map.Entry<PGMMap, PGMMap.MapLogRecord> entry, int index) {
        return entry.getValue().getLegacyFormattedMessage();
      }

      @Override
      public String formatEmpty() {
        return ChatColor.GREEN
            + AllTranslations.get().translate("command.development.listErrors.noErrors", sender);
      }
    }.display(audience, errors.entries(), page);
  }

  @Command(
          name = "loadnewmaps",
          aliases = {"findnewmaps", "newmaps", "findmaps"},
          desc = "Scan for new maps and load them")
  public static void loadNewMaps(CommandSender sender, Audience audience) {
    sender.sendMessage(ChatColor.WHITE + "Scanning for new maps...");
    // Clear errors for maps that failed to load, because we want to see those errors again
    PGM.get().getMapErrorTracker().clearErrorsExcept(PGM.get().getMapLibrary().getMaps());

    try {
      final Collection<PGMMap> newMaps = PGM.get().getMatchManager().loadNewMaps();

      if (newMaps.isEmpty()) {
        sender.sendMessage(ChatColor.WHITE + "No new maps found");
      } else if (newMaps.size() == 1) {
        sender.sendMessage(
            ChatColor.WHITE
                + "Found new map "
                + ChatColor.YELLOW
                + newMaps.iterator().next().getInfo().name);
      } else {
        sender.sendMessage(
            ChatColor.WHITE
                + "Found "
                + ChatColor.AQUA
                + newMaps.size()
                + ChatColor.WHITE
                + " new maps");
      }
    } catch (MapNotFoundException e) {
      audience.sendWarning(new PersonalizedText("No maps found at all"), false);
    }
  }

  @Command(
          name = "features",
          desc = "Lists all features by ID and type",
          descFooter = "<page>")
  public static void featuresCommand(Match match, MatchPlayer player, int page)
  {
    new PrettyPaginatedResult<Feature>("Defined Features") {
      @Override
      public String format(Feature feature, int i) {
        return (i + 1)
            + ". "
            + ChatColor.GOLD
            + feature.getId()
            + ChatColor.GRAY
            + " - "
            + ChatColor.RED
            + feature.getClass().getSimpleName();
      }
    }.display(player, match.getFeatureContext().getAll(), page);
  }

  @Command(
          name = "feature",
          aliases = {"fl"},
          desc = "Prints information regarding a specific feature",
          descFooter = "[feature]")
  public static void featureCommand(CommandSender sender, Match match, @Text String featureQuery) {
    if (match == null || match.getFeatureContext().get(featureQuery) == null) {
      sender.sendMessage(
          ChatColor.RED
              + "No feature by the name of "
              + ChatColor.GOLD
              + featureQuery
              + ChatColor.RED
              + " was found.");
    } else {
      Feature feature = match.getFeatureContext().get(featureQuery);
      sender.sendMessage(
          ChatColor.GOLD
              + featureQuery
              + ChatColor.GRAY
              + " corresponds to: "
              + ChatColor.WHITE
              + feature.toString());
    }
  }

  @Command(
          name = "velocity",
          aliases = {"vel"},
          desc = "Apply a velocity to a player",
          descFooter = "<player> [velocity]")
  public static void velocity(CommandSender sender, Player target, Vector velocity) {
    sender.sendMessage(
        String.format(
            "Applying velocity (%.2f, %.2f, %.2f) to " + target.getName(sender),
            velocity.getX(),
            velocity.getY(),
            velocity.getZ()));
    target.setVelocity(velocity);
    target.updateVelocity();
  }

  @Command(
          name = "filter",
          aliases = {"fil"},
          desc = "Query a filter by ID with yourself or the given player",
          descFooter = "<filter> <player>")
  public static void filter(
      CommandSender sender, MatchPlayer self, Match match, String id, Player player)
  {
    MatchPlayer target = match.getPlayer(player);
    FilterDefinition filter = getFilterDefinition(id, match, sender);

    Filter.QueryResponse response = filter.query(target.getQuery());
    String out =
        ChatColor.BLUE
            + id
            + ChatColor.DARK_GRAY
            + "("
            + ChatColor.GRAY
            + target.getBukkit().getName(sender)
            + ChatColor.DARK_GRAY
            + ") -> ";

    switch (response) {
      case DENY:
        out += ChatColor.DARK_RED;
        break;
      case ABSTAIN:
        out += ChatColor.YELLOW;
        break;
      case ALLOW:
        out += ChatColor.GREEN;
        break;
    }

    sender.sendMessage(out + response.name());
  }

  @Command(
          name = "updatemap",
          aliases = {"savemap"},
          desc = "Updates the original map file to changes in game")
  public static void updateMap(CommandSender sender, Match match, @Switch(name = 'f', desc = "force") boolean force)
  {
    final PGMMap map = match.getMap();
    final Logger logger = map.getLogger();

    if (match.getPhase() != MatchPhase.IDLE && !force) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.map.update.running", sender));
    } else {
      World world = match.getWorld();
      logger.info("Saving world");
      world.save();

      final Path worldFolder = world.getWorldFolder().toPath();

      try {
        // Prune void regions from the world
        Path regionFolder = worldFolder.resolve("region");
        File[] regionFiles = regionFolder.toFile().listFiles();
        if (regionFiles == null) {
          logger.info("No region folder");
        } else {
          logger.info("Pruning empty regions");
          for (File file : regionFiles) {
            Matcher matcher = Pattern.compile("r\\.(-?\\d+).(-?\\d+).mca").matcher(file.getName());
            if (!matcher.matches()) continue;

            int regionX = Integer.parseInt(matcher.group(1));
            int regionZ = Integer.parseInt(matcher.group(2));
            int minX = regionX << 5;
            int minZ = regionZ << 5;
            int maxX = minX + 32;
            int maxZ = minZ + 32;
            boolean empty = true;

            for (int x = minX; x < maxX; x++) {
              for (int z = minZ; z < maxZ; z++) {
                if (!NMSHacks.isChunkEmpty(world.getChunkAt(x, z))) empty = false;
              }
            }

            if (empty) {
              logger.info("  empty: " + file.getName());
              if (!file.delete()) {
                throw new UnknownError(
                    AllTranslations.get()
                        .translate("command.map.update.deleteFailed", sender, file.getName()));
              }
            } else {
              logger.info("  non-empty: " + file.getName());
            }
          }
        }

        final Path sourceWorldFolder = map.getFolder().getAbsolutePath();
        Path sourceRegionFolder = sourceWorldFolder.resolve("region");
        Path sourceRegionBackup = sourceWorldFolder.resolve("region_backup");

        if (Files.exists(sourceRegionFolder)) {
          logger.info("Copying source /region to /region_backup");
          Files.copy(sourceRegionFolder, sourceRegionBackup, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Searching for changed files");
        Files.walkFileTree(
            worldFolder,
            new SimpleFileVisitor<Path>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                  throws IOException {
                Path relativePath = worldFolder.relativize(file);
                File sourceFile = sourceWorldFolder.resolve(relativePath).toFile();
                // Always copy region files (mca)
                if (!sourceFile.exists() && !file.getFileName().toString().endsWith(".mca")) {
                  logger.info("  skipping: " + relativePath);
                } else if (FileUtils.contentEquals(file.toFile(), sourceFile)) {
                  logger.info("  unchanged: " + relativePath);
                } else {
                  logger.info("  changed: " + relativePath);
                  Files.copy(file, sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
              }
            });

        if (Files.exists(sourceRegionBackup)) {
          logger.info("Deleting region backup");
          Files.delete(sourceRegionBackup);
        }
      } catch (IOException e) {
        throw new RuntimeException(e.toString());
      }

      sender.sendMessage(
          ChatColor.GREEN + AllTranslations.get().translate("command.map.update.success", sender));
    }
  }

  private static FilterDefinition getFilterDefinition(String id, Match match, CommandSender sender)
  {
    FilterDefinition feature = match.getMapContext().features().get(id, FilterDefinition.class);
    if (feature == null) {
      throw new NoSuchElementException("No " + FilterDefinition.class.getSimpleName() + " with ID " + id);
    }
    return feature;
  }

  private static Multimap<PGMMap, PGMMap.MapLogRecord> getErrors() {
    return PGM.get().getMapErrorTracker().getErrors();
  }
}

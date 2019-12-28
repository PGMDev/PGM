package tc.oc.pgm.rotation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.PGMMap;

/**
 * Manages all the existing {@link Rotation}s, as for maintaining their order, and updating the one
 * {@link PGM} will use after every match depending on the player count (Dynamic Rotations)
 */
public class RotationManager implements PGMMapOrder {

  private Logger logger;

  private File rotationsFile;
  private FileConfiguration rotationsFileConfiguration;
  private List<Rotation> rotations = new ArrayList<>();
  private Rotation activeRotation;
  /** When a {@link PGMMap} is manually set next, it overrides the rotation order * */
  private PGMMap overriderMap;

  public RotationManager(Logger logger, File rotationsFile) {
    this.rotationsFile = rotationsFile;
    this.logger = logger;

    if (!rotationsFile.exists()) {
      try {
        FileUtils.copyInputStreamToFile(PGM.get().getResource("rotations.yml"), rotationsFile);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create the rotations.yml file", e);
        return;
      }
    }

    this.rotationsFileConfiguration = YamlConfiguration.loadConfiguration(rotationsFile);
    loadRotations();
  }

  private void loadRotations() {
    rotations =
        rotationsFileConfiguration.getConfigurationSection("rotations").getKeys(false).stream()
            .map(
                key ->
                    new Rotation(
                        this,
                        rotationsFileConfiguration.getConfigurationSection("rotations." + key),
                        key))
            .filter(Rotation::isEnabled)
            .collect(Collectors.toList());

    Rotation lastActiveRotation =
        getRotationByName(rotationsFileConfiguration.getString("last_active"));
    setActiveRotation(lastActiveRotation);
  }

  public void saveRotations() {
    try {
      rotationsFileConfiguration.save(rotationsFile);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not save next map for future reference", e);
    }
  }

  private void setActiveRotation(Rotation activeRotation) {
    this.activeRotation = activeRotation;
  }

  public Rotation getActiveRotation() {
    return activeRotation;
  }

  public List<Rotation> getRotations() {
    return rotations;
  }

  public void matchEnded(Match match) {
    int activePlayers = match.getPlayers().size() - (match.getObservers().size() / 2);

    rotations.stream()
        .filter(rot -> activePlayers >= rot.getPlayers())
        .max(Rotation::compareTo)
        .ifPresent(this::updateActiveRotation);
  }

  private void updateActiveRotation(Rotation rotation) {
    if (rotation == activeRotation) return;

    setActiveRotation(rotation);

    rotationsFileConfiguration.set("last_active", activeRotation.getName());
    saveRotations();

    Bukkit.broadcastMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + ChatColor.GREEN
            + AllTranslations.get()
                .translate(
                    "rotations.rotationChange",
                    Bukkit.getConsoleSender(),
                    (ChatColor.AQUA + rotation.getName() + ChatColor.GREEN)));
  }

  private Rotation getRotationByPlayerCount(int playerCount) {
    return rotations.stream()
        .filter(rotation -> rotation.getPlayers() == playerCount)
        .findFirst()
        .orElse(null);
  }

  /**
   * Method to be kept for the future, as it's very useful and could be used for a variety of
   * things.
   *
   * @param name The name of the desired rotation
   * @return The {@link Rotation} which matches the input name
   */
  public Rotation getRotationByName(String name) {
    return rotations.stream()
        .filter(rot -> rot.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  @Override
  public PGMMap popNextMap() {
    if (overriderMap != null) {
      PGMMap overrider = overriderMap;
      overriderMap = null;
      return overrider;
    }
    return activeRotation.popNextMap();
  }

  @Override
  public PGMMap getNextMap() {
    if (overriderMap != null) return overriderMap;
    if (activeRotation != null) return activeRotation.getNextMap();
    return null;
  }

  @Override
  public void setNextMap(PGMMap map) {
    overriderMap = map;
  }
}

package tc.oc.pgm.rotation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.map.PGMMap;

/**
 * Manages all the existing {@link FixedPGMMapOrder}s (Rotations), as for maintaining their order,
 * and updating the one {@link PGM} will use after every match depending on the player count
 * (Dynamic Rotations)
 */
public class FixedPGMMapOrderManager implements PGMMapOrder {

  private MatchManager matchManager;
  private Logger logger;

  private File rotationsFile;
  private FileConfiguration rotationsFileConfiguration;
  private List<FixedPGMMapOrder> rotations = new ArrayList<>();
  private FixedPGMMapOrder activeRotation;
  private boolean isEvaluatingPlayerCount = true;
  /** When a {@link PGMMap} is manually set next, it overrides the rotation order * */
  private PGMMap overriderMap;

  public FixedPGMMapOrderManager(MatchManager matchManager, Logger logger, File rotationsFile) {
    this.matchManager = matchManager;
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
    rotationsFileConfiguration
        .getConfigurationSection("rotations")
        .getKeys(false)
        .forEach(
            key ->
                rotations.add(
                    new FixedPGMMapOrder(
                        rotationsFileConfiguration.getConfigurationSection("rotations." + key),
                        key)));

    rotations.forEach(FixedPGMMapOrder::load);

    rotations.forEach(
        rotation -> {
          if (!rotation.isEnabled()) rotations.remove(rotation);
        });

    FixedPGMMapOrder lastActiveRotation =
        rotations.stream()
            .map(FixedPGMMapOrder::getName)
            .filter(name -> name.equals(rotationsFileConfiguration.getString("last_active")))
            .map(this::getRotationByName)
            .findFirst()
            .orElse(null);

    setActiveRotation(lastActiveRotation);
  }

  private void setActiveRotation(FixedPGMMapOrder activeRotation) {
    this.activeRotation = activeRotation;
  }

  public FixedPGMMapOrder getActiveRotation() {
    return activeRotation;
  }

  void setEvaluatingPlayerCount(boolean evaluatingPlayerCount) {
    this.isEvaluatingPlayerCount = evaluatingPlayerCount;
  }

  public boolean isEvaluatingPlayerCount() {
    return isEvaluatingPlayerCount;
  }

  public List<FixedPGMMapOrder> getRotations() {
    return rotations;
  }

  public void saveCurrentPosition() {
    rotationsFileConfiguration.set(
        "rotations." + activeRotation.getName() + ".next_map",
        activeRotation.getMapInPosition(activeRotation.getNextPosition()).getName());

    try {
      rotationsFileConfiguration.save(rotationsFile);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not save next map for future reference", e);
    }
  }

  public void recalculateActiveRotation() {
    int activePlayers = getActivePlayers();

    FixedPGMMapOrder newRotation =
        rotations.stream()
            .filter(FixedPGMMapOrder::isEnabled)
            .map(FixedPGMMapOrder::getPlayers)
            .sorted()
            .filter(playerCount -> playerCount >= activePlayers)
            .map(this::getRotationByPlayerCount)
            .findFirst()
            .orElse(null);

    updateActiveRotation(newRotation);
  }

  private int getActivePlayers() {
    Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
    if (onlinePlayers.isEmpty()) return 0;

    Match match = matchManager.getMatch((Player) onlinePlayers.toArray()[0]);

    assert match != null;
    return onlinePlayers.size() - match.getObservers().size() / 2;
  }

  private void updateActiveRotation(FixedPGMMapOrder rotation) {
    if (rotation == activeRotation) return;

    setActiveRotation(rotation);

    rotationsFileConfiguration.set("last_active", activeRotation.getName());

    try {
      rotationsFileConfiguration.save(rotationsFile);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not save last active rotation for future reference.", e);
    }

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

  private FixedPGMMapOrder getRotationByPlayerCount(int playerCount) {
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
   * @return The {@link FixedPGMMapOrder} (Rotation) which matches the input name
   */
  public FixedPGMMapOrder getRotationByName(String name) {
    return rotations.stream().filter(rot -> rot.getName().equals(name)).findFirst().orElse(null);
  }

  @Override
  public PGMMap popNextMap() {
    if (overriderMap != null) {
      PGMMap overrider = overriderMap;
      overriderMap = null;
      return overrider;
    } else {
      saveCurrentPosition();
      return activeRotation.popNextMap();
    }
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

package tc.oc.pgm.rotation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.map.PGMMap;

public class RotationManager implements PGMMapOrderProvider {

  private MatchManager matchManager;

  private File rotationsFile;
  private List<Rotation> rotations = new ArrayList<>();
  private Rotation activeRotation;
  private boolean isEvaluatingPlayerCount = true;
  /** When a {@link PGMMap} is manually set next, it overrides the rotation order * */
  private PGMMap overriderMap;

  public RotationManager(MatchManager matchManager, Logger logger, File rotationsFile) {
    this.matchManager = matchManager;
    this.rotationsFile = rotationsFile;

    if (!rotationsFile.exists()) {
      try {
        FileUtils.copyInputStreamToFile(
            PGM.GLOBAL.get().getResource("rotations.yml"), rotationsFile);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create the rotations.yml file", e);
      }
    }

    loadRotations();
  }

  private void loadRotations() {
    FileConfiguration rotationsFileConfiguration =
        YamlConfiguration.loadConfiguration(rotationsFile);

    rotationsFileConfiguration
        .getKeys(false)
        .forEach(
            key ->
                rotations.add(
                    new Rotation(rotationsFileConfiguration.getConfigurationSection(key), key)));
    rotations.forEach(Rotation::load);

    rotations.forEach(
        rotation -> {
          if (!rotation.isEnabled()) rotations.remove(rotation);
        });

    setActiveRotation(getRotationByName("default"));
  }

  private void setActiveRotation(Rotation activeRotation) {
    this.activeRotation = activeRotation;
  }

  public Rotation getActiveRotation() {
    return activeRotation;
  }

  void setEvaluatingPlayerCount(boolean evaluatingPlayerCount) {
    this.isEvaluatingPlayerCount = evaluatingPlayerCount;
  }

  public boolean isEvaluatingPlayerCount() {
    return isEvaluatingPlayerCount;
  }

  public List<Rotation> getRotations() {
    return rotations;
  }

  private void saveCurrentPosition() {
    FileConfiguration rotationFileConfiguration =
        YamlConfiguration.loadConfiguration(rotationsFile);
    rotationFileConfiguration.set(
        activeRotation.getName() + ".position", activeRotation.getPosition());

    try {
      rotationFileConfiguration.save(rotationsFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void recalculateActiveRotation() {
    int activePlayers = getActivePlayers();
    int[] playerCounts = new int[rotations.size()];

    AtomicInteger count = new AtomicInteger(0);
    rotations.forEach(
        rotation -> {
          if (rotation.isEnabled()) {
            playerCounts[count.get()] = rotation.getPlayers();
            count.getAndIncrement();
          }
        });

    int distance = Math.abs(playerCounts[0] - activePlayers);
    int accuratePlayerCountIndex = 0;

    for (int i = 1; i < playerCounts.length; i++) {
      int idistance = Math.abs(playerCounts[i] - activePlayers);
      if (idistance < distance) {
        accuratePlayerCountIndex = i;
        distance = idistance;
      }
    }

    updateRotation(playerCounts, accuratePlayerCountIndex);
  }

  private int getActivePlayers() {
    Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
    if (onlinePlayers.isEmpty()) return 0;

    Match match = matchManager.getMatch((Player) onlinePlayers.toArray()[0]);

    assert match != null;
    return onlinePlayers.size() - match.getObservers().size() / 2;
  }

  private void updateRotation(final int[] playerCounts, final int accuratePlayerCountIndex) {
    rotations.forEach(
        rotation -> {
          if (rotation.getPlayers() == playerCounts[accuratePlayerCountIndex]
              && activeRotation.getPlayers() != playerCounts[accuratePlayerCountIndex]) {
            setActiveRotation(rotation);
            Bukkit.broadcastMessage(
                ChatColor.WHITE
                    + "["
                    + ChatColor.GOLD
                    + "Rotations"
                    + ChatColor.WHITE
                    + "] "
                    + ChatColor.GREEN
                    + "Rotation has been set to "
                    + ChatColor.AQUA
                    + rotation.getName()
                    + ChatColor.GREEN
                    + " in order to better adjust to the current player count.");
          }
        });
  }

  public Rotation getRotationByName(String name) {
    AtomicReference<Rotation> matchByName = new AtomicReference<>();

    getRotations()
        .forEach(
            rotation -> {
              if (rotation.getName().equals(name)) {
                matchByName.set(rotation);
              }
            });

    return matchByName.get();
  }

  public PGMMap getInitialMap() {
    return activeRotation.getMapInPosition(activeRotation.getPosition());
  }

  @Override
  public PGMMap popNextMap() {
    if (overriderMap == null) {
      saveCurrentPosition();
      return activeRotation.popNextMap();
    } else {
      PGMMap overrider = overriderMap;
      overriderMap = null;
      return overrider;
    }
  }

  @Override
  public PGMMap getNextMap() {
    if (activeRotation != null) {
      if (overriderMap == null) {
        return activeRotation.getNextMap();
      } else return overriderMap;
    } else return popFallbackMap();
  }

  @Override
  public PGMMap popFallbackMap() {
    Iterator<Match> iterator = matchManager.getMatches().iterator();
    PGMMap current = iterator.hasNext() ? iterator.next().getMap() : null;

    List<PGMMap> maps = new ArrayList<>(PGM.GLOBAL.get().getMapLibrary().getMaps());
    PGMMap next;
    do {
      Collections.shuffle(maps);
      next = maps.get(0);
    } while (maps.size() > 1 && Objects.equals(current, next));

    return next;
  }

  @Override
  public void setNextMap(PGMMap map) {
    overriderMap = map;
  }
}

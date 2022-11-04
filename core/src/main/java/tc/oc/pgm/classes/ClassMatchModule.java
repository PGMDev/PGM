package tc.oc.pgm.classes;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;
import static tc.oc.pgm.util.text.TextException.exception;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

@ListenerScope(MatchScope.RUNNING)
public class ClassMatchModule implements MatchModule, Listener {
  private final Match match;
  private final String family;
  private final Map<String, PlayerClass> classes;
  private final Set<PlayerClass> classesByName;
  private final PlayerClass defaultClass;

  private final Map<UUID, PlayerClass> selectedClasses = Maps.newHashMap();
  private final Map<UUID, PlayerClass> lastPlayedClass = Maps.newHashMap();

  public ClassMatchModule(
      Match match, String family, Map<String, PlayerClass> classes, PlayerClass defaultClass) {
    this.match = match;
    this.family = assertNotNull(family, "family");
    this.classes = assertNotNull(classes, "classes");
    this.defaultClass = assertNotNull(defaultClass, "default class");

    this.classesByName =
        Sets.newTreeSet(
            new Comparator<PlayerClass>() {
              @Override
              public int compare(PlayerClass o1, PlayerClass o2) {
                return o1.getName().compareTo(o2.getName());
              }
            });
    this.classesByName.addAll(this.classes.values());
  }

  @Override
  public void unload() {
    selectedClasses.clear();
    lastPlayedClass.clear();
  }

  /**
   * Gets the class family.
   *
   * <p>Family is used for grouping classes into a set of similar classes.
   *
   * @return class family
   */
  public String getFamily() {
    return this.family;
  }

  /**
   * Gets the default class that players will have if none selected.
   *
   * @return default class
   */
  public PlayerClass getDefaultClass() {
    return this.defaultClass;
  }

  /**
   * Gets the set of classes that are present in alphabetical order by name.
   *
   * @return set of classes present
   */
  public Set<PlayerClass> getClasses() {
    return classesByName;
  }

  /**
   * Gets the player class by the given search term.
   *
   * @param search search term
   * @return class where the name exactly matches the given search term
   */
  public @Nullable PlayerClass getPlayerClass(String search) {
    return this.classes.get(search);
  }

  /**
   * Gets the class that the given player has chosen to be on next respawn, which is not necessarily
   * the class that they are currently playing as.
   *
   * @param userId player to look up
   * @return player's class or the default class if none selected
   */
  public PlayerClass getSelectedClass(UUID userId) {
    PlayerClass cls = this.selectedClasses.get(userId);
    return cls != null ? cls : this.defaultClass;
  }

  /** Get the last class that the given player spawned as. */
  public PlayerClass getLastPlayedClass(UUID userId) {
    PlayerClass cls = this.lastPlayedClass.get(userId);
    return cls != null ? cls : this.defaultClass;
  }

  /** Get the class that given player is spawned as or will spawn as next */
  public PlayerClass getPlayingClass(UUID userId) {
    MatchPlayer player = match.getPlayer(userId);
    if (player != null && player.isAlive()) {
      return getLastPlayedClass(userId);
    } else {
      return getSelectedClass(userId);
    }
  }

  /**
   * Gets all players who currently have the given class.
   *
   * @param cls class of which to fetch members
   * @return set of players (some of which may be offline) who have the given class
   */
  public Set<UUID> getClassMembers(PlayerClass cls) {
    Set<UUID> result = Sets.newHashSet();

    for (Map.Entry<UUID, PlayerClass> entry : this.selectedClasses.entrySet()) {
      if (entry.getValue().equals(cls)) {
        result.add(entry.getKey());
      }
    }

    return result;
  }

  /**
   * Get whether the given player can change classes.
   *
   * @param userId player to check
   * @return true if the player can change classes, false otherwise
   */
  public boolean getCanChangeClass(UUID userId) {
    PlayerClass cls = this.lastPlayedClass.get(userId);
    return cls == null || !cls.isSticky();
  }

  /**
   * Sets the given player's class to the one indicated.
   *
   * @param userId player to set the class
   * @param cls class to set
   * @return old class or default if none selected
   * @throws tc.oc.pgm.util.text.TextException if the player may not change classes
   */
  public PlayerClass setPlayerClass(UUID userId, PlayerClass cls) {
    assertNotNull(userId, "player id");
    assertNotNull(cls, "player class");
    assertTrue(this.classes.containsValue(cls), "class is not valid for this match");

    if (!this.getCanChangeClass(userId)) {
      throw exception("match.class.sticky");
    }

    PlayerClass oldClass = this.selectedClasses.put(userId, cls);
    if (oldClass == null) oldClass = this.defaultClass;

    MatchPlayer matchPlayer = this.match.getPlayer(userId);
    if (matchPlayer != null) {
      this.match.callEvent(new PlayerClassChangeEvent(matchPlayer, this.family, oldClass, cls));
    }

    return oldClass;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerSpawn(ParticipantSpawnEvent event) {
    this.lastPlayedClass.put(
        event.getPlayer().getId(), getSelectedClass(event.getPlayer().getId()));
  }

  public void giveClassKits(MatchPlayer player) {
    for (Kit kit : getSelectedClass(player.getId()).getKits()) {
      player.applyKit(kit, true);
    }
  }
}

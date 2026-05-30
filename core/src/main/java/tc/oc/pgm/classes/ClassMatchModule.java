package tc.oc.pgm.classes;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;
import static tc.oc.pgm.util.text.TextException.exception;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

@ListenerScope(MatchScope.LOADED)
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

    this.classesByName = Sets.newTreeSet(Comparator.comparing(PlayerClass::getName));
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
   * Gets the number of players on a specific party/team who are currently playing as or
   * have selected a given class.
   *
   * @param party party to check
   * @param cls class to count
   * @return amount of players currently occupying this class slot
   */
  public long getPartyClassCount(Party party, PlayerClass cls) {
    return party.getPlayers().stream()
        .filter(player -> getPlayingClass(player.getId()).equals(cls))
        .count();
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

    MatchPlayer matchPlayer = this.match.getPlayer(userId);
    if (matchPlayer != null && matchPlayer.getParty() != null && cls.getMax() > 0 && !cls.equals(this.defaultClass)) {
      
      if (!matchPlayer.getParty().isParticipating() || matchPlayer.isDead()) {
        PlayerClass oldClass = this.selectedClasses.put(userId, cls);
        if (oldClass == null) oldClass = this.defaultClass;
        this.match.callEvent(new PlayerClassChangeEvent(matchPlayer, this.family, oldClass, cls));
        return oldClass;
      }

      PlayerClass currentPlaying = getPlayingClass(userId);
      PlayerClass currentSelected = this.selectedClasses.get(userId);

      if (!cls.equals(currentPlaying) && !cls.equals(currentSelected)) {
        int currentCount = (int) this.getPartyClassCount(matchPlayer.getParty(), cls);

        if (currentCount >= cls.getMax()) {
          throw exception("match.class.full", Component.text(cls.getMax()));
        }
      }
    }

    PlayerClass oldClass = this.selectedClasses.put(userId, cls);
    if (oldClass == null) oldClass = this.defaultClass;
    
    if (matchPlayer != null) {
      this.match.callEvent(new PlayerClassChangeEvent(matchPlayer, this.family, oldClass, cls));
    }

    return oldClass;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerSpawn(ParticipantSpawnEvent event) {
    MatchPlayer player = event.getPlayer();
    UUID uuid = player.getId();
    Party party = player.getParty();
    PlayerClass selectedClass = getSelectedClass(uuid);

    if (party != null && party.isParticipating() && !selectedClass.equals(this.defaultClass) && selectedClass.getMax() > 0) {
      long currentCount = this.getPartyClassCount(party, selectedClass);

      if (currentCount >= selectedClass.getMax()) {
        PlayerClass prevClass = this.lastPlayedClass.get(uuid);

        if (prevClass != null && !prevClass.equals(this.defaultClass)) {
          this.selectedClasses.put(uuid, prevClass);
          player.sendWarning(translatable("match.class.revert", Component.text(prevClass.getName())));
        } else {
          this.selectedClasses.put(uuid, this.defaultClass);
          player.sendWarning(translatable("match.class.reset", Component.text(this.defaultClass.getName())));
        }
      }
    }

    this.lastPlayedClass.put(uuid, getSelectedClass(uuid));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChangeParty(tc.oc.pgm.events.PlayerChangePartyEvent event) {
    MatchPlayer player = event.getPlayer();
    Party newParty = event.getNewParty();

    if (player != null && newParty != null && event.isParticipating()) {
      PlayerClass selectedClass = this.getSelectedClass(player.getId());

      if (!selectedClass.equals(this.defaultClass) && selectedClass.getMax() > 0) {
        int currentCount = (int) this.getPartyClassCount(newParty, selectedClass);

        if (currentCount >= selectedClass.getMax()) {
          this.selectedClasses.put(player.getId(), this.defaultClass);
          player.sendWarning(translatable("match.class.reset", Component.text(this.defaultClass.getName())));
        }
      }
    }
  }

  public void giveClassKits(MatchPlayer player) {
    for (Kit kit : getSelectedClass(player.getId()).getKits()) {
      player.applyKit(kit, true);
    }
  }
}

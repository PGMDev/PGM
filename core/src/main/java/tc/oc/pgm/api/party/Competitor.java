package tc.oc.pgm.api.party;

import org.bukkit.scoreboard.NameTagVisibility;

/** A party that can participate in a match. */
public interface Competitor extends Party {
  /**
   * Gets an immutable, unique identifier.
   *
   * @return an identifier
   */
  String getId();

  /**
   * Gets the name tag visibility among members.
   *
   * @return a visibility
   */
  NameTagVisibility getNameTagVisibility();

  /**
   * Sets an override for name tag visibility
   *
   * @param visibility A {@link NameTagVisibility}
   */
  void setNameTagVisibility(NameTagVisibility visibility);
}

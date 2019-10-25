package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Party;

/** Called when the name of a team is changed. */
public class PartyRenameEvent extends PartyEvent {

  private final String oldName;
  private final String newName;

  public PartyRenameEvent(Party party, String oldName, String newName) {
    super(party);
    this.oldName = checkNotNull(oldName, "old name");
    this.newName = checkNotNull(newName, "new name");
  }

  /**
   * Gets the old name of the team.
   *
   * @return Old name
   */
  public String getOldName() {
    return this.oldName;
  }

  /**
   * Gets the new name of the team.
   *
   * @return New name
   */
  public String getNewName() {
    return this.newName;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}

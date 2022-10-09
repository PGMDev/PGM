package tc.oc.pgm.api.party.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Party;

/** Called when a {@link Party}'s name is changed. */
public class PartyRenameEvent extends PartyEvent {

  private final String oldName;
  private final String newName;

  public PartyRenameEvent(Party party, String oldName, String newName) {
    super(party);
    this.oldName = assertNotNull(oldName, "old name");
    this.newName = assertNotNull(newName, "new name");
  }

  /**
   * Get the old name of the {@link Party}.
   *
   * @return The old name.
   */
  public final String getOldName() {
    return this.oldName;
  }

  /**
   * Get the new, and current, name of the {@link Party}.
   *
   * @return The current name.
   */
  public final String getNewName() {
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

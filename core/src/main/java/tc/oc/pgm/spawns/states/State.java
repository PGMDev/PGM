package tc.oc.pgm.spawns.states;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.event.player.PlayerAttackEntityEvent;

public abstract class State {

  protected final SpawnMatchModule smm;
  protected final MatchPlayer player;
  protected final Player bukkit;

  protected StatePermissions permission = null;

  private boolean entered, exited;

  public State(SpawnMatchModule smm, MatchPlayer player) {
    this.smm = smm;
    this.player = player;
    this.bukkit = player.getBukkit();
  }

  public boolean isCurrent() {
    return entered && !exited;
  }

  public void enterState() {
    if (exited) {
      throw new IllegalStateException("Tried to enter already exited state " + this);
    } else if (entered) {
      throw new IllegalStateException("Tried to enter already entered state " + this);
    }
    entered = true;

    if (permission != null) permission.givePermission(player);
  }

  /**
   * @param events List of events to call AFTER the transition is complete. This method can add
   *     events to the list, and the caller will fire them. Events that can generate another state
   *     transition for the same player MUST be deferred in this way, as state transitions cannot be
   *     nested within each other.
   */
  public void leaveState(List<Event> events) {
    if (permission != null) permission.revokePermission(player);

    if (!entered) {
      throw new IllegalStateException("Tried to leave state before entering " + this);
    } else if (exited) {
      throw new IllegalStateException("Tried to leave already exited state " + this);
    }
    exited = true;
  }

  protected void transition(State newState) {
    smm.transition(player, this, newState);
  }

  public void onEvent(final PlayerDeathEvent event) {
    // Prevents the default death handling, in case a player manages to die
    // while not in the Alive state, or without generating a PGMPlayerDeathEvent
    // at all. This can happen from e.g. the /slay command.
    bukkit.setHealth(player.getBukkit().getMaxHealth());
    event.getDrops().clear();
  }

  public void tick() {}

  public void onEvent(final PlayerJoinPartyEvent event) {}

  public void onEvent(final MatchPlayerDeathEvent event) {}

  public void onEvent(final InventoryClickEvent event) {}

  public void onEvent(final ObserverInteractEvent event) {}

  public void onEvent(final PlayerAttackEntityEvent event) {}

  public void onEvent(final PlayerItemTransferEvent event) {}

  public void onEvent(final EntityDamageEvent event) {}

  public void onEvent(final MatchStartEvent event) {}

  public void onEvent(final MatchFinishEvent event) {}
}

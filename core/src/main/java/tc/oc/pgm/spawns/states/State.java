package tc.oc.pgm.spawns.states;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.PlayerChangePartyEvent;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.event.player.PlayerAttackEntityEvent;

public abstract class State {

  protected final Match match;
  protected final SpawnMatchModule smm;
  protected final MatchPlayer player;
  protected final Player bukkit;

  protected StatePermissions permission = null;

  private boolean entered, exited;

  public State(MatchPlayer player) {
    this.match = player.getMatch();
    this.smm = match.needModule(SpawnMatchModule.class);
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

  public void leaveState() {
    if (permission != null) permission.revokePermission(player);

    if (!entered) {
      throw new IllegalStateException("Tried to leave state before entering " + this);
    } else if (exited) {
      throw new IllegalStateException("Tried to leave already exited state " + this);
    }
    exited = true;
  }

  protected void transition(State newState) {
    smm.transition(player, newState);
  }

  public void onEvent(final PlayerDeathEvent event) {
    // Prevents the default death handling, in case a player manages to die
    // while not in the Alive state, or without generating a PGMPlayerDeathEvent
    // at all. This can happen from e.g. the /slay command.
    bukkit.setHealth(player.getBukkit().getMaxHealth());
    event.getDrops().clear();
  }

  public void tick() {}

  /** Called only when oldParty and newParty are both non-null */
  public void onEvent(final PlayerChangePartyEvent event) {}

  public void onEvent(final MatchPlayerDeathEvent event) {}

  public void onEvent(final InventoryClickEvent event) {}

  public void onEvent(final ObserverInteractEvent event) {}

  public void onEvent(final PlayerAttackEntityEvent event) {}

  public void onEvent(final PlayerItemTransferEvent event) {}

  public void onEvent(final EntityDamageEvent event) {}

  public void onEvent(final MatchStartEvent event) {}

  public void onEvent(final MatchFinishEvent event) {}
}

package tc.oc.pgm.spawns.states;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAttackEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.event.PlayerItemTransferEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;

/** Player is waiting to spawn as a participant */
public abstract class Spawning extends Participating {

  protected boolean spawnRequested;

  public Spawning(SpawnMatchModule smm, MatchPlayer player) {
    super(smm, player);
    this.spawnRequested = options.auto;
  }

  @Override
  public void enterState() {
    super.enterState();

    player.setDead(true);
    player.resetGamemode();
  }

  public void requestSpawn() {
    this.spawnRequested = true;
  }

  @Override
  public void onEvent(PlayerInteractEvent event) {
    super.onEvent(event);
    event.setCancelled(true);
    if (event.getAction() == Action.LEFT_CLICK_AIR
        || event.getAction() == Action.LEFT_CLICK_BLOCK) {
      requestSpawn();
    }
  }

  @Override
  public void onEvent(PlayerAttackEntityEvent event) {
    super.onEvent(event);
    event.setCancelled(true);
    requestSpawn();
  }

  @Override
  public void onEvent(final PlayerItemTransferEvent event) {
    super.onEvent(event);
    event.setCancelled(true);
  }

  @Override
  public void tick() {
    if (!trySpawn()) {
      updateTitle();
    }

    super.tick();
  }

  protected boolean trySpawn() {
    if (!spawnRequested) return false;

    Spawn spawn = chooseSpawn();
    if (spawn == null) return false;

    Location location = spawn.getSpawn(player);
    if (location == null) return false;

    transition(new Alive(smm, player, spawn, location));
    return true;
  }

  public @Nullable Spawn chooseSpawn() {
    if (spawnRequested) {
      return smm.chooseSpawn(player);
    } else {
      return null;
    }
  }

  public void updateTitle() {
    player.showTitle(getTitle(), new PersonalizedText(getSubtitle(), ChatColor.GREEN), 0, 3, 3);
  }

  protected abstract Component getTitle();

  protected Component getSubtitle() {
    if (!spawnRequested) {
      return new PersonalizedTranslatable("death.respawn.unconfirmed");
    } else if (options.message != null) {
      return options.message;
    } else {
      return new PersonalizedTranslatable("death.respawn.confirmed.waiting");
    }
  }
}

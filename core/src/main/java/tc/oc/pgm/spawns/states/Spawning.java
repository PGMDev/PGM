package tc.oc.pgm.spawns.states;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;

import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAttackEntityEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

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
    player.resetInteraction();
    player.resetVisibility();
  }

  public void requestSpawn() {
    this.spawnRequested = true;
  }

  @Override
  public void onEvent(ObserverInteractEvent event) {
    super.onEvent(event);
    requestSpawn();
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
  public void onEvent(EntityDamageEvent event) {
    super.onEvent(event);
    event.setCancelled(true);
  }

  @Override
  public void tick() {
    if (!trySpawn()) {
      updateTitle();
      if (player.isLegacy()) sendMessage();
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

  public void sendMessage() {}

  public void updateTitle() {
    player.showTitle(
        title(
            getTitle(),
            getSubtitle().color(NamedTextColor.GREEN),
            Title.Times.of(Duration.ZERO, fromTicks(3), fromTicks(3))));
  }

  protected abstract Component getTitle();

  protected Component getSubtitle() {
    if (!spawnRequested) {
      return translatable("death.respawn.unconfirmed");
    } else if (options.message != null) {
      return options.message;
    } else {
      return translatable("death.respawn.confirmed.waiting");
    }
  }
}

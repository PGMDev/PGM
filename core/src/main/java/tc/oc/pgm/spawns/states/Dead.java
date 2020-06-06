package tc.oc.pgm.spawns.states;

import java.util.List;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.spawns.events.DeathKitApplyEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.nms.NMSHacks;

/** Player is waiting to respawn after dying in-game */
public class Dead extends Spawning {
  private static final long CORPSE_ROT_TICKS = 15;

  private final long deathTick;
  private boolean kitted, rotted;

  public Dead(SpawnMatchModule smm, MatchPlayer player) {
    this(smm, player, player.getMatch().getTick().tick);
  }

  public Dead(SpawnMatchModule smm, MatchPlayer player, long deathTick) {
    super(smm, player);
    this.deathTick = deathTick;
  }

  @Override
  public void enterState() {
    super.enterState();

    player.resetInventory();

    if (player.isVisible()) NMSHacks.playDeathAnimation(player.getBukkit());

    if (!options.spectate) player.setFrozen(true);

    // Show red vignette
    NMSHacks.showBorderWarning(player.getBukkit(), true);
  }

  @Override
  public void leaveState(List<Event> events) {
    player.setFrozen(false);
    player.setDead(false);
    NMSHacks.showBorderWarning(bukkit, false);

    super.leaveState(events);
  }

  protected long age() {
    return player.getMatch().getTick().tick - deathTick;
  }

  @Override
  public void tick() {
    long age = age();

    if (!kitted && ticksUntilRespawn() <= 0) {
      this.kitted = true;
      // Give the player the team/class picker, after death has cleared their inventory
      player.getMatch().callEvent(new DeathKitApplyEvent(player));
      bukkit.updateInventory();
    }

    if (!rotted && age >= CORPSE_ROT_TICKS) {
      this.rotted = true;
      // Make player invisible after the death animation is complete
      player.setVisible(false);
      player.resetGamemode();
    }

    super.tick(); // May transition to a different state, so call last
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    super.onEvent(event);
    if (!(event.getNewParty() instanceof Competitor)) {
      transition(new Observing(smm, player, true, false));
    }
  }

  protected long ticksUntilRespawn() {
    return Math.max(0, options.delayTicks - age());
  }

  @Override
  public @Nullable Spawn chooseSpawn() {
    if (ticksUntilRespawn() > 0) {
      return null;
    } else {
      return super.chooseSpawn();
    }
  }

  public void requestSpawn() {
    if (player.getMatch().getTick().tick - deathTick
        >= TimeUtils.toTicks(SpawnModule.IGNORE_CLICKS_DELAY)) {
      super.requestSpawn();
    }
  }

  @Override
  protected Component getTitle() {
    return TranslatableComponent.of("deathScreen.title", TextColor.RED);
  }

  @Override
  protected Component getSubtitle() {
    long ticks = ticksUntilRespawn();
    if (ticks > 0) {
      return TranslatableComponent.of(
          spawnRequested ? "death.respawn.confirmed.time" : "death.respawn.unconfirmed.time",
          TextComponent.of(String.format("%.1f", (ticks / (float) 20)), TextColor.AQUA));
    } else {
      return super.getSubtitle();
    }
  }

  @Override
  public void onEvent(InventoryClickEvent event) {
    super.onEvent(event);
    event.setCancelled(true); // don't allow inventory changes when dead
  }

  @Override
  public void onEvent(EntityDamageEvent event) {
    super.onEvent(event);
    event.setCancelled(true);
  }
}

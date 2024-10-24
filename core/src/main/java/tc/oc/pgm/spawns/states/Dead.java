package tc.oc.pgm.spawns.states;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.nms.Packets.PLAYERS;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.spawns.events.DeathKitApplyEvent;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.PotionEffects;
import tc.oc.pgm.util.named.NameStyle;

/** Player is waiting to respawn after dying in-game */
public class Dead extends Spawning {
  private static final long CORPSE_ROT_TICKS = 20;

  private static final PotionEffect CONFUSION =
      new PotionEffect(PotionEffects.NAUSEA, 100, 0, true, false);
  private static final PotionEffect BLINDNESS_SHORT =
      new PotionEffect(PotionEffects.BLINDNESS, 21, 0, true, false);
  private static final PotionEffect BLINDNESS_LONG =
      new PotionEffect(PotionEffects.BLINDNESS, Integer.MAX_VALUE, 0, true, false);

  private boolean kitted, rotted;

  public Dead(SpawnMatchModule smm, MatchPlayer player) {
    super(smm, player, player.getMatch().getTick().tick, 0);
  }

  @Override
  public void enterState() {
    super.enterState();

    player.resetInventory();

    if (player.isVisible()) PLAYERS.playDeathAnimation(player.getBukkit());

    if (!options.spectate) player.setFrozen(true);

    // Show red vignette
    PLAYERS.showBorderWarning(player.getBukkit(), true);

    // Flash/wobble the screen. If we don't delay this then the client glitches out
    // when the player dies from a potion effect. I have no idea why it happens,
    // but this fixes it. We could investigate a better fix at some point.
    smm.getMatch().getExecutor(MatchScope.LOADED).execute(() -> {
      if (isCurrent() && bukkit.isOnline()) {
        bukkit.addPotionEffect(options.blackout ? BLINDNESS_LONG : BLINDNESS_SHORT, true);
        bukkit.addPotionEffect(CONFUSION, true);
      }
    });
  }

  @Override
  public void leaveState(List<Event> events) {
    player.setFrozen(false);
    player.setDead(false);

    PLAYERS.showBorderWarning(bukkit, false);

    bukkit.removePotionEffect(PotionEffects.BLINDNESS);
    bukkit.removePotionEffect(PotionEffects.NAUSEA);

    // If regular rotting didn't end, force-finish it to avoid client-side
    endRotting();

    super.leaveState(events);
  }

  @Override
  public void tick() {
    long age = age();

    if (!kitted
        && ticksUntilRespawn() <= 0
        && age >= TimeUtils.toTicks(SpawnModule.MIN_KIT_DELAY)) {
      this.kitted = true;
      // Give the player the team/class picker, after death has cleared their inventory
      player.getMatch().callEvent(new DeathKitApplyEvent(player));
      bukkit.updateInventory();
    }

    // Make player invisible after the death animation is complete
    if (age >= CORPSE_ROT_TICKS) endRotting();

    super.tick(); // May transition to a different state, so call last
  }

  private void endRotting() {
    if (this.rotted) return;
    this.rotted = true;
    player.setVisible(false);
    player.resetVisibility();
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    super.onEvent(event);
    if (!(event.getNewParty() instanceof Competitor)) {
      transition(new Observing(smm, player, true, false));
    }
  }

  public void requestSpawn() {
    if (age() >= TimeUtils.toTicks(SpawnModule.IGNORE_CLICKS_DELAY)) {
      super.requestSpawn();
    }
  }

  @Override
  protected Component getTitle(boolean spectator) {
    return translatable(
        "deathScreen.title" + (spectator ? ".spectator" : ""),
        NamedTextColor.RED,
        player(player, NameStyle.SIMPLE_COLOR));
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

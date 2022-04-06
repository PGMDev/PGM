package tc.oc.pgm.spawns.states;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.named.NameStyle;
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
  private static final long CORPSE_ROT_TICKS = 20;

  private static final PotionEffect CONFUSION =
      new PotionEffect(PotionEffectType.CONFUSION, 100, 0, true, false);
  private static final PotionEffect BLINDNESS_SHORT =
      new PotionEffect(PotionEffectType.BLINDNESS, 21, 0, true, false);
  private static final PotionEffect BLINDNESS_LONG =
      new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, true, false);

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

    // Flash/wobble the screen. If we don't delay this then the client glitches out
    // when the player dies from a potion effect. I have no idea why it happens,
    // but this fixes it. We could investigate a better fix at some point.
    smm.getMatch()
        .getExecutor(MatchScope.LOADED)
        .execute(
            () -> {
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

    NMSHacks.showBorderWarning(bukkit, false);

    bukkit.removePotionEffect(PotionEffectType.BLINDNESS);
    bukkit.removePotionEffect(PotionEffectType.CONFUSION);

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
      player.resetVisibility();
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
  protected Component getTitle(boolean spectator) {
    return translatable(
        "deathScreen.title" + (spectator ? ".spectator" : ""),
        NamedTextColor.RED,
        player(this.player.getId(), NameStyle.SIMPLE_COLOR));
  }

  @Override
  protected Component getSubtitle(boolean spectator) {
    long ticks = ticksUntilRespawn();
    if (ticks > 0) {
      return translatable(
          spawnRequested
              ? "death.respawn.confirmed.time"
              : "death.respawn.unconfirmed.time" + (spectator ? ".spectator" : ""),
          NamedTextColor.GREEN,
          text(String.format("%.1f", (ticks / (float) 20)), NamedTextColor.AQUA));
    } else {
      return super.getSubtitle(spectator);
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

  @Override
  public void sendMessage() {
    long ticks = options.delayTicks - age();
    if (ticks % (ticks > 0 ? 20 : 100) == 0) {
      player.sendMessage(
          (ticks > 0
                  ? translatable(
                      spawnRequested
                          ? "death.respawn.confirmed.time"
                          : "death.respawn.unconfirmed.time",
                      text((int) (ticks / (float) 20), NamedTextColor.AQUA))
                  : super.getSubtitle(false))
              .color(NamedTextColor.GREEN));
    }
  }
}

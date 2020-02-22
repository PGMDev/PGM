package tc.oc.pgm.spawns.states;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.killreward.KillRewardMatchModule;
import tc.oc.pgm.modules.ItemKeepMatchModule;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

/** Player is alive and participating */
public class Alive extends Participating {

  protected final Spawn spawn;
  protected final Location location;

  public Alive(SpawnMatchModule smm, MatchPlayer player, Spawn spawn, Location location) {
    super(smm, player);
    this.spawn = spawn;
    this.location = location;
  }

  @Override
  public void enterState() {
    super.enterState();

    player.reset();
    player.setDead(false);
    player.resetGamemode();

    // Fire Bukkit's event
    PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(player.getBukkit(), location, false);
    player.getMatch().callEvent(respawnEvent);

    // Fire our event
    ParticipantSpawnEvent spawnEvent =
        new ParticipantSpawnEvent(player, respawnEvent.getRespawnLocation());
    player.getMatch().callEvent(spawnEvent);

    // Teleport the player
    player.getBukkit().teleport(spawnEvent.getLocation());

    // Return kept items
    // TODO: Module should do this itself, maybe from ParticipantSpawnEvent
    ItemKeepMatchModule ikmm = player.getMatch().getModule(ItemKeepMatchModule.class);
    if (ikmm != null) {
      ikmm.restoreKeptArmor(player);
      ikmm.restoreKeptInventory(player);
    }

    player.setVisible(true);
    player.resetGamemode();

    // Apply spawn kit
    spawn.applyKit(player);

    // Apply class kit(s)
    // TODO: Module should do this itself, maybe from ParticipantSpawnEvent
    ClassMatchModule cmm = player.getMatch().getModule(ClassMatchModule.class);
    if (cmm != null) {
      cmm.giveClassKits(player);
    }

    // Give kill rewards earned while dead
    // TODO: Module should do this itself, maybe from ParticipantSpawnEvent
    KillRewardMatchModule kwmm = player.getMatch().getModule(KillRewardMatchModule.class);
    if (kwmm != null) {
      kwmm.giveDeadPlayerRewards(player);
    }

    player.getBukkit().updateInventory();
  }

  @Override
  public void leaveState(List<Event> events) {
    events.add(new ParticipantDespawnEvent(player, player.getBukkit().getLocation()));
    super.leaveState(events);
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    super.onEvent(event);

    if (event.getNewParty() instanceof Competitor) {
      transition(new Joining(smm, player));
    } else {
      transition(new Observing(smm, player, true, true));
    }
  }

  @Override
  public void onEvent(PlayerDeathEvent event) {
    // Prevent default death, but allow item drops
    bukkit.setHealth(player.getBukkit().getMaxHealth());
  }

  @Override
  public void onEvent(MatchPlayerDeathEvent event) {
    if (!event.isPredicted()) {
      die(event.getKiller());
    }
  }

  public void die(@Nullable ParticipantState killer) {
    player.setDead(true);

    // setting a player's gamemode resets their fall distance
    // we need the fall distance for the death message
    // we set the fall distance back to 0 when we refresh the player
    float fallDistance = bukkit.getFallDistance();
    player.resetGamemode();
    bukkit.setFallDistance(fallDistance);

    playDeathEffect(killer);

    transition(new Dead(smm, player));
  }

  private void playDeathEffect(@Nullable ParticipantState killer) {
    playDeathSound(killer);

    // negative health boost potions sometimes change max health
    for (PotionEffect effect : bukkit.getActivePotionEffects()) {
      // Keep speed and NV for visual continuity
      if (effect.getType() != null
          && !PotionEffectType.NIGHT_VISION.equals(effect.getType())
          && !PotionEffectType.SPEED.equals(effect.getType())) {

        bukkit.removePotionEffect(effect.getType());
      }
    }

    // Flash/wobble the screen. If we don't delay this then the client glitches out
    // when the player dies from a potion effect. I have no idea why it happens,
    // but this fixes it. We could investigate a better fix at some point.
    smm.getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTask(
            new Runnable() {
              @Override
              public void run() {
                if (bukkit.isOnline()) {
                  bukkit.addPotionEffect(
                      new PotionEffect(
                          PotionEffectType.BLINDNESS,
                          options.blackout ? Integer.MAX_VALUE : 21,
                          0,
                          true,
                          false),
                      true);
                  bukkit.addPotionEffect(
                      new PotionEffect(PotionEffectType.CONFUSION, 100, 0, true, false), true);
                }
              }
            });
  }

  private void playDeathSound(@Nullable ParticipantState killer) {
    Vector death = player.getBukkit().getLocation().toVector();

    for (MatchPlayer listener : player.getMatch().getPlayers()) {
      if (listener == player) {
        // Own death is normal pitch, full volume
        listener.playSound(new Sound("mob.irongolem.death"));
      } else if (killer != null && killer.isPlayer(listener)) {
        // Kill is higher pitch, quieter
        listener.playSound(new Sound("mob.irongolem.death", 0.75f, 4f / 3f));
      } else if (listener.getParty() == player.getParty()) {
        // Ally death is a shorter sound
        listener.playSound(new Sound("mob.irongolem.hit", death));
      } else {
        // Enemy death is higher pitch
        listener.playSound(new Sound("mob.irongolem.hit", 1, 4f / 3f, death));
      }
    }
  }
}

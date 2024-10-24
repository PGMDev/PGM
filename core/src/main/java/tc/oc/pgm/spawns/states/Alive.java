package tc.oc.pgm.spawns.states;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
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
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.util.bukkit.Sounds;

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
    player.resetInteraction();

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
    player.resetVisibility();
    player.setGameMode(GameMode.SURVIVAL);
    bukkit.setAllowFlight(false);

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

    // Call when it is safe to apply extra kits
    ParticipantKitApplyEvent kitEvent = new ParticipantKitApplyEvent(player);
    player.getMatch().callEvent(kitEvent);

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
      transition(new Joining(smm, player, smm.getJoinPenalty(event)));
    } else {
      transition(new Observing(smm, player, true, true));
    }
  }

  @Override
  public void onEvent(PlayerDeathEvent event) {
    // Prevent default death, but allow item drops
    bukkit.setMaxHealth(20);
    bukkit.setHealth(bukkit.getMaxHealth());
  }

  @Override
  public void onEvent(MatchPlayerDeathEvent event) {
    if (!event.isPredicted()) {
      die(event.getKiller());
    }
    playDeathSound(event.getKiller());
  }

  public void die(@Nullable ParticipantState killer) {
    player.setDead(true);

    // Setting a player's gamemode resets their fall distance.
    // We need the fall distance for the death message.
    // We set the fall distance back to 0 when we refresh the player.
    float fallDistance = bukkit.getFallDistance();
    bukkit.setGameMode(GameMode.CREATIVE);
    bukkit.setFallDistance(fallDistance);

    playDeathEffect(killer);

    transition(new Dead(smm, player));
  }

  private void playDeathEffect(@Nullable ParticipantState killer) {
    // negative health boost potions sometimes change max health
    for (PotionEffect effect : bukkit.getActivePotionEffects()) {
      // Keep speed and NV for visual continuity
      if (effect.getType() != null
          && !PotionEffectType.NIGHT_VISION.equals(effect.getType())
          && !PotionEffectType.SPEED.equals(effect.getType())) {

        bukkit.removePotionEffect(effect.getType());
      }
    }
  }

  private void playDeathSound(@Nullable ParticipantState killer) {
    Vector death = player.getBukkit().getLocation().toVector();

    for (MatchPlayer listener : player.getMatch().getPlayers()) {
      if (listener == player) {
        listener.playSound(Sounds.DEATH_OWN);
      } else if (killer != null && killer.isPlayer(listener)) {
        listener.playSound(Sounds.DEATH_KILLED);
      } else if (listener.getParty() == player.getParty()) {
        listener.playSound(Sounds.DEATH_ALLY, death.getX(), death.getY(), death.getZ());
      } else {
        listener.playSound(Sounds.DEATH_ENEMY, death.getX(), death.getY(), death.getZ());
      }
    }
  }
}

package tc.oc.pgm.doublejump;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerResetEvent;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

@ListenerScope(MatchScope.RUNNING)
public class DoubleJumpMatchModule implements MatchModule, Listener, Tickable {

  private static final Sound ZOMBIE_INFECT =
      parse(Sound::valueOf, "ZOMBIE_INFECT", "ENTITY_ZOMBIE_INFECT");

  private static class Jumper {
    private final Player player;
    private final DoubleJumpKit kit;
    private float charge;

    private Jumper(Player player, DoubleJumpKit kit) {
      this.player = player;
      this.kit = kit;
    }
  }

  private final Match match;
  private final OnlinePlayerMapAdapter<Jumper> jumpers;

  public DoubleJumpMatchModule(Match match) {
    this.match = match;
    this.jumpers = new OnlinePlayerMapAdapter<>(PGM.get());
    this.jumpers.enable();
  }

  @Override
  public void tick(Match match, Tick tick) {
    for (Map.Entry<Player, Jumper> entry : jumpers.entrySetCopy()) {
      Player player = entry.getKey();
      Jumper jumper = entry.getValue();
      if (player.isOnGround() || jumper.kit.rechargeInAir || jumper.charge > 0f) {
        setCharge(jumper, jumper.charge + jumper.kit.chargePerTick());
        refreshJump(player);
      }
    }
  }

  @Override
  public void disable() {
    for (Iterator<Player> iterator = this.jumpers.keySet().iterator(); iterator.hasNext(); ) {
      Player player = iterator.next();
      iterator.remove();
      this.refreshJump(player);
    }
    this.jumpers.disable();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerReset(PlayerResetEvent event) {
    this.setKit(event.getPlayer().getBukkit(), null);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    this.setKit(event.getEntity(), null);
  }

  public void setKit(Player player, @Nullable DoubleJumpKit kit) {
    if (kit != null && kit.enabled) {
      Jumper jumper = new Jumper(player, kit);
      this.jumpers.put(player, jumper);
      this.setCharge(jumper, 1f);
    } else {
      this.jumpers.remove(player);
      this.removeCharge(player);
      this.refreshJump(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleFlight(final PlayerToggleFlightEvent event) {
    Player player = event.getPlayer();
    Jumper jumper = this.jumpers.get(player);
    if (jumper == null) return;

    if (event.isFlying()) {
      event.setCancelled(true);

      this.setCharge(jumper, 0f);
      this.refreshJump(player);

      // calculate jump
      Vector impulse = player.getLocation().getDirection();

      impulse.setY(0.75 + Math.abs(impulse.getY()) * 0.5);
      impulse.multiply(jumper.kit.power / 3f);
      event.getPlayer().setVelocity(impulse);

      player.getWorld().playSound(player.getLocation(), ZOMBIE_INFECT, 0.5f, 1.8f);
    }
  }

  private void setCharge(Jumper jumper, float charge) {
    if (jumper.kit.needsRecharge()) {
      charge = Math.min(charge, 1.0f);
      if (charge != jumper.charge) {
        jumper.charge = charge;
        jumper.player.setExp(jumper.charge);
      }
    }
  }

  private void removeCharge(Player player) {
    player.setExp(0f);
  }

  private void refreshJump(Player player) {
    if (player.getGameMode() != GameMode.CREATIVE) {
      player.setAllowFlight(this.canJump(player));
    }
  }

  private boolean canJump(Player player) {
    Jumper jumper = this.jumpers.get(player);
    return jumper != null && (!jumper.kit.needsRecharge() || jumper.charge >= 1f);
  }

  public boolean hasKit(MatchPlayer player) {
    return jumpers.containsKey(player.getBukkit());
  }
}

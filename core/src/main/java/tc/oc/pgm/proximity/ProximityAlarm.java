package tc.oc.pgm.proximity;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.util.chat.Sound;

public class ProximityAlarm implements Listener {
  private static final long MESSAGE_INTERVAL = 5000;
  private static final float FLARE_CHANCE = 0.25f;

  private static final Sound SOUND = new Sound("fireworks.largeBlast_far", 1f, 0.7f);

  protected final Random random;
  protected final Match match;
  protected final ProximityAlarmDefinition definition;
  protected final Set<MatchPlayer> playersInside = Sets.newHashSet();
  protected long lastMessageTime = 0;

  public ProximityAlarm(Match match, ProximityAlarmDefinition definition, Random random) {
    this.random = random;
    this.match = match;
    this.definition = definition;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final CoarsePlayerMoveEvent event) {
    updatePlayer(this.match.getPlayer(event.getPlayer()), event.getTo());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSpawn(final ParticipantSpawnEvent event) {
    updatePlayer(event.getPlayer(), event.getPlayer().getBukkit().getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
    this.playersInside.remove(event.getPlayer());
  }

  private void updatePlayer(MatchPlayer player, Location location) {
    if (player != null
        && player.canInteract()
        && this.definition.detectFilter.query(player.getQuery()).isAllowed()) {
      if (!player.isDead() && this.definition.detectRegion.contains(location.toVector())) {
        this.playersInside.add(player);
      } else {
        this.playersInside.remove(player);
      }
    }
  }

  public void showAlarm() {
    if (this.random.nextFloat() < FLARE_CHANCE) {
      if (!this.playersInside.isEmpty()) {
        this.showFlare();
        this.showMessage();
      }
    }
  }

  private void showFlare() {
    Vector pos = this.definition.detectRegion.getBounds().getCenterPoint();
    float angle = (float) (this.random.nextFloat() * Math.PI * 2);

    pos.add(
        new Vector(
            Math.sin(angle) * this.definition.flareRadius,
            0,
            Math.cos(angle) * this.definition.flareRadius));

    Set<Color> colors = new HashSet<>();

    for (MatchPlayer player : this.playersInside) {
      colors.add(player.getParty().getFullColor());
    }
  }

  private void showMessage() {
    if (this.definition.alertMessage == null) {
      return;
    }

    long now = System.currentTimeMillis();
    if (this.lastMessageTime + MESSAGE_INTERVAL < now) {
      this.lastMessageTime = now;

      for (MatchPlayer player : this.match.getPlayers()) {
        if (this.definition.alertFilter.query(player.getQuery()).isAllowed()) {
          player.sendMessage(ChatColor.RED + this.definition.alertMessage);
          player.playSound(SOUND);
        }
      }
    }
  }
}

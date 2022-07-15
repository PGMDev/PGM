package tc.oc.pgm.portals;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.util.nms.NMSHacks;

public class Portal implements FeatureDefinition {

  protected final PortalTransform transform;
  protected final Filter trigger;
  protected final Filter participantFilter;
  protected final Filter observerFilter;
  protected final boolean sound;
  protected final boolean smooth;

  public Portal(
      Filter trigger,
      PortalTransform transform,
      Filter participantFilter,
      Filter observerFilter,
      boolean sound,
      boolean smooth) {

    this.trigger = trigger;
    this.transform = transform;
    this.participantFilter = participantFilter;
    this.observerFilter = observerFilter;
    this.sound = sound;
    this.smooth = smooth;
  }

  public void load(FilterMatchModule fmm) {
    fmm.onRise(
        MatchPlayer.class,
        trigger,
        matchPlayer -> {
          if (matchPlayer != null
              && canUse(matchPlayer)
              && !PortalMatchModule.teleported(matchPlayer)) {
            teleportPlayer(matchPlayer, matchPlayer.getBukkit().getLocation());
          }
        });
  }

  protected boolean canUse(MatchPlayer player) {
    return (player.isParticipating() ? participantFilter : observerFilter)
        .query(player)
        .isAllowed();
  }

  protected void teleportPlayer(final MatchPlayer player, final Location from) {
    final Location to = transform.apply(from);
    final Player bukkit = player.getBukkit();
    final Match match = player.getMatch();

    final Vector delta;
    final float deltaYaw, deltaPitch;
    if (this.smooth) {
      Location location = bukkit.getLocation();
      delta = to.toVector().subtract(location.toVector());
      deltaYaw = to.getYaw() - location.getYaw();
      deltaPitch = to.getPitch() - location.getPitch();
    } else {
      delta = null;
      deltaYaw = deltaPitch = Float.NaN;
    }

    if (this.sound) {
      // Don't play the sound for the teleporting player at the entering portal,
      // because they will instantly teleport away and hear the one at the exit.
      for (MatchPlayer listener : match.getPlayers()) {
        if (listener != player && listener.getBukkit().canSee(player.getBukkit())) {
          final Location loc = bukkit.getLocation();
          listener.playSound(
              sound(key("mob.endermen.portal"), Sound.Source.MASTER, 1f, 1f),
              loc.getX(),
              loc.getY(),
              loc.getZ());
        }
      }
    }

    // Use ENDER_PEARL as the cause so that this teleport is treated as an "in-game" movement
    if (delta == null) {
      bukkit.teleport(to, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
    } else {
      NMSHacks.teleportRelative(
          bukkit, delta, deltaYaw, deltaPitch, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
    }

    // Reset fall distance
    bukkit.setFallDistance(0);

    if (this.sound) {
      for (MatchPlayer listener : match.getPlayers()) {
        if (listener.getBukkit().canSee(player.getBukkit())) {
          listener.playSound(
              sound(key("mob.endermen.portal"), Sound.Source.MASTER, 1f, 1f),
              to.getX(),
              to.getY(),
              to.getZ());
        }
      }
    }
  }
}

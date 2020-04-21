package tc.oc.pgm.portals;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.chat.Sound;

public class Portal implements FeatureDefinition {
  protected final Region region;
  protected final @Nullable Region destinationRegion;
  protected final Filter filter;
  protected final boolean sound;
  protected final boolean protect;
  protected final boolean bidirectional;
  protected final boolean smooth;

  protected final boolean relative;

  protected final DoubleProvider dx;
  protected final DoubleProvider dy;
  protected final DoubleProvider dz;
  protected final DoubleProvider dYaw;
  protected final DoubleProvider dPitch;

  protected final RelativeDoubleProvider idx;
  protected final RelativeDoubleProvider idy;
  protected final RelativeDoubleProvider idz;
  protected final RelativeDoubleProvider idYaw;
  protected final RelativeDoubleProvider idPitch;

  public Portal(
      Region region,
      DoubleProvider dx,
      DoubleProvider dy,
      DoubleProvider dz,
      DoubleProvider dYaw,
      DoubleProvider dPitch,
      @Nullable Region destinationRegion,
      Filter filter,
      boolean sound,
      boolean protect,
      boolean bidirectional,
      boolean smooth) {

    this.region = region;
    this.destinationRegion = destinationRegion;
    this.filter = filter;
    this.sound = sound;
    this.protect = protect;
    this.bidirectional = bidirectional;
    this.smooth = smooth;
    this.relative =
        dx instanceof RelativeDoubleProvider
            && dy instanceof RelativeDoubleProvider
            && dz instanceof RelativeDoubleProvider
            && dYaw instanceof RelativeDoubleProvider
            && dPitch instanceof RelativeDoubleProvider;

    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.dYaw = dYaw;
    this.dPitch = dPitch;

    if (this.relative) {
      this.idx = ((RelativeDoubleProvider) dx).inverse();
      this.idy = ((RelativeDoubleProvider) dy).inverse();
      this.idz = ((RelativeDoubleProvider) dz).inverse();
      this.idYaw = ((RelativeDoubleProvider) dYaw).inverse();
      this.idPitch = ((RelativeDoubleProvider) dPitch).inverse();

    } else {
      if (this.bidirectional) {
        throw new IllegalArgumentException(
            "Bidirectional portals must use relative destination coordinates");
      }

      this.idx = null;
      this.idy = null;
      this.idz = null;
      this.idYaw = null;
      this.idPitch = null;
    }
  }

  public Region getRegion() {
    return this.region;
  }

  public @Nullable Region getDestinationRegion() {
    return destinationRegion;
  }

  public Filter getFilter() {
    return this.filter;
  }

  public boolean isProtected() {
    return protect;
  }

  protected boolean canUse(MatchPlayer player) {
    return player.getParty().isObserving() || this.filter.query(player.getQuery()).isAllowed();
  }

  protected Location cloneWith(Location original, Vector position, float yaw, float pitch) {
    return new Location(
        original.getWorld(), position.getX(), position.getY(), position.getZ(), yaw, pitch);
  }

  protected Vector inverseTransform(Vector old) {
    Preconditions.checkState(this.relative, "inverse transform requires relative coordinates");
    return new Vector(this.idx.get(old.getX()), this.idy.get(old.getY()), this.idz.get(old.getZ()));
  }

  protected Location inverseTransform(Location old) {
    Preconditions.checkState(this.relative, "inverse transform requires relative coordinates");
    return cloneWith(
        old,
        this.inverseTransform(old.toVector()),
        (float) this.idYaw.get(old.getYaw()),
        (float) this.idPitch.get(old.getPitch()));
  }

  protected Vector transform(Vector old) {
    return new Vector(this.dx.get(old.getX()), this.dy.get(old.getY()), this.dz.get(old.getZ()));
  }

  protected Location transform(Location old) {
    return cloneWith(
        old,
        this.transform(old.toVector()),
        (float) this.dYaw.get(old.getYaw()),
        (float) this.dPitch.get(old.getPitch()));
  }

  protected Location transformToRegion(Location old, Region region) {
    return cloneWith(
        old,
        region.getRandom(PGM.get().getMatchManager().getMatch(old.getWorld()).getRandom()),
        (float) this.dYaw.get(old.getYaw()),
        (float) this.dPitch.get(old.getPitch()));
  }

  protected void teleportPlayer(final MatchPlayer player, final Location destination) {
    final Player bukkit = player.getBukkit();
    final Location destinationClone = destination.clone();
    final Match match = player.getMatch();

    final Vector delta;
    final float deltaYaw, deltaPitch;
    if (this.smooth) {
      Location location = bukkit.getLocation();
      delta = destination.toVector().subtract(location.toVector());
      deltaYaw = destination.getYaw() - location.getYaw();
      deltaPitch = destination.getPitch() - location.getPitch();
    } else {
      delta = null;
      deltaYaw = deltaPitch = Float.NaN;
    }

    if (this.sound) {
      // Don't play the sound for the teleporting player at the entering portal,
      // because they will instantly teleport away and hear the one at the exit.
      for (MatchPlayer listener : match.getPlayers()) {
        if (listener != player && listener.getBukkit().canSee(player.getBukkit())) {
          listener.playSound(
              new Sound("mob.endermen.portal", 1f, 1f, bukkit.getLocation().toVector()));
        }
      }
    }

    // Defer because some things break if a player teleports during a move event
    match
        .getExecutor(MatchScope.LOADED)
        .execute(
            () -> {
              if (!bukkit.isOnline()) return;

              // Use ENDER_PEARL as the cause so that this teleport is treated
              // as an "in-game" movement
              if (delta == null) {
                bukkit.teleport(destinationClone, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
              } else {
                bukkit.teleportRelative(
                    delta, deltaYaw, deltaPitch, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
              }

              // Reset fall distance
              bukkit.setFallDistance(0);

              if (Portal.this.sound) {
                for (MatchPlayer listener : match.getPlayers()) {
                  if (listener.getBukkit().canSee(player.getBukkit())) {
                    listener.playSound(
                        new Sound("mob.endermen.portal", 1f, 1f, destinationClone.toVector()));
                  }
                }
              }
            });
  }

  /**
   * Teleport the given player if they will have stepped in the portal when moving from oldPosition
   * to newPosition.
   *
   * @return true if the player was teleported, false otherwise
   */
  public boolean teleportEligiblePlayer(
      MatchPlayer player, Vector oldPosition, Vector newPosition, Location playerLocation) {
    if (this.canUse(player)) {
      if (this.region.enters(oldPosition, newPosition)) {
        if (this.destinationRegion != null) {
          this.teleportPlayer(
              player, this.transformToRegion(playerLocation, this.destinationRegion));
          return true;
        } else {
          this.teleportPlayer(player, this.transform(playerLocation));
          return true;
        }

      } else if (this.bidirectional) {
        if (this.destinationRegion != null
            && this.destinationRegion.enters(oldPosition, newPosition)) {
          this.teleportPlayer(player, this.transformToRegion(playerLocation, this.region));
          return true;
        } else if (this.region.enters(
            this.inverseTransform(oldPosition), this.inverseTransform(newPosition))) {
          this.teleportPlayer(player, this.inverseTransform(playerLocation));
          return true;
        }
      }
    }

    return false;
  }
}

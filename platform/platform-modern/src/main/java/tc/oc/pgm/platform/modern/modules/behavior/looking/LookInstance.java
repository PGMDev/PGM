package tc.oc.pgm.platform.modern.modules.behavior.looking;

import io.papermc.paper.entity.LookAnchor;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class LookInstance {

  private final Mannequin mannequin;
  private final LookBehavior behavior;
  private final float restYaw;
  private final float restPitch;

  public LookInstance(Mannequin mannequin, LookBehavior behavior) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.restYaw = behavior.getRestYaw() != null ? behavior.getRestYaw() : mannequin.getYaw();
    this.restPitch =
        behavior.getRestPitch() != null ? behavior.getRestPitch() : mannequin.getPitch();
  }

  public void tick(Match match) {
    Location eyes = null;
    if (behavior.getTrackPlayer()) {
      eyes = nearestPlayerInRange(match);
    } else if (behavior.getTrackPoint() != null) {
      eyes = behavior.getTrackPoint().toLocation(mannequin.getEntity().getWorld());
    }

    if (eyes != null) {
      if (behavior.getRotation() == RotationType.BODY) {
        mannequin.getEntity().lookAt(eyes, LookAnchor.EYES);
      } else {
        lookHeadOnly(eyes);
      }
    }
  }

  private @Nullable Location nearestPlayerInRange(Match match) {
    double rangeSq = behavior.getRange() * behavior.getRange();
    Location self = mannequin.getLocation();
    MatchPlayer nearest = null;
    double best = rangeSq;
    for (MatchPlayer player : match.getParticipants()) {
      Player bukkit = player.getBukkit();
      if (bukkit == null || player.isDead()) continue;
      double d = bukkit.getLocation().distanceSquared(self);
      if (d <= best) {
        best = d;
        nearest = player;
      }
    }
    return nearest != null ? nearest.getBukkit().getEyeLocation() : null;
  }

  private void lookHeadOnly(Location eyes) {
    Location self = mannequin.getEntity().getEyeLocation();
    double dx = eyes.getX() - self.getX();
    double dy = eyes.getY() - self.getY();
    double dz = eyes.getZ() - self.getZ();

    float desiredYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
    float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    float delta = wrapDegrees(desiredYaw - restYaw);
    float clampedYaw = restYaw + Math.max(-90f, Math.min(90f, delta));

    mannequin.getEntity().setRotation(clampedYaw, pitch);
  }

  private static float wrapDegrees(float deg) {
    deg %= 360f;
    if (deg >= 180f) deg -= 360f;
    if (deg < -180f) deg += 360f;
    return deg;
  }
}

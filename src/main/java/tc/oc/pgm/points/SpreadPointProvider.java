package tc.oc.pgm.points;

import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;

/** Get 16 points from each child and choose the farthest point from any enemy */
public class SpreadPointProvider extends AggregatePointProvider {

  private static final int SAMPLE_COUNT = 16;

  public SpreadPointProvider(Collection<? extends PointProvider> children) {
    super(children);
  }

  @Override
  public Location getPoint(Match match, @Nullable Entity entity) {
    Location bestPoint = null;
    double bestDistance = Double.NEGATIVE_INFINITY;
    MatchPlayer player = match.getPlayer(entity);

    for (int i = 0; i < SAMPLE_COUNT; i++) {
      for (PointProvider child : children) {
        Location pos = child.getPoint(match, entity);
        if (pos == null) continue;

        double nearest = Double.POSITIVE_INFINITY;

        for (MatchPlayer enemy : match.getParticipatingPlayers()) {
          if (enemy.isParticipating()
              && !enemy.isDead()
              && (player == null || player.getParty() != enemy.getParty())) {
            nearest = Math.min(nearest, pos.distanceSquared(enemy.getBukkit().getLocation()));
          }
        }

        if (bestDistance < nearest) {
          bestDistance = nearest;
          bestPoint = pos;
        }
      }
    }

    return bestPoint;
  }

  @Override
  public boolean canFail() {
    return allChildrenCanFail();
  }
}

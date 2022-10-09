package tc.oc.pgm.points;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

/** Get 16 points from each child and choose the farthest point from any enemy */
public class SpreadPointProvider extends AggregatePointProvider {

  private static final int SAMPLE_COUNT = 16;

  private final boolean spreadTeammates;

  public SpreadPointProvider(
      Collection<? extends PointProvider> children, boolean spreadTeammates) {
    super(children);
    this.spreadTeammates = spreadTeammates;
  }

  @Override
  public Location getPoint(Match match, @Nullable Entity entity) {
    List<Location> bestPoints = new ArrayList<>(SAMPLE_COUNT);
    double bestDistance = Double.NEGATIVE_INFINITY;
    MatchPlayer player = match.getPlayer(entity);

    for (int i = 0; i < SAMPLE_COUNT; i++) {
      for (PointProvider child : children) {
        Location pos = child.getPoint(match, entity);
        if (pos == null) continue;

        double nearest = Double.POSITIVE_INFINITY;

        for (MatchPlayer enemy : match.getParticipants()) {
          if (enemy.isParticipating()
              && !enemy.isDead()
              && (player == null
                  || player.getParty() != enemy.getParty()
                  || this.spreadTeammates)) {
            nearest = Math.min(nearest, pos.distanceSquared(enemy.getBukkit().getLocation()));
          }
        }

        if (bestDistance <= nearest) {
          if (bestDistance != nearest) {
            bestPoints.clear();
          }
          bestDistance = nearest;
          bestPoints.add(pos);
        }
      }
    }

    int index = (int) Math.floor(match.getRandom().nextInt(bestPoints.size()));
    return bestPoints.get(index);
  }

  @Override
  public boolean canFail() {
    return allChildrenCanFail();
  }
}

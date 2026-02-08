package tc.oc.pgm.stats;

import java.util.Collection;
import lombok.Getter;

// Holds calculated total stats for a single team
@Getter
public class TeamStats implements StatHolder {

  private int teamKills = 0;
  private int teamDeaths = 0;
  private double damageDone = 0;
  private double damageTaken = 0;
  private double bowDamage = 0;
  private double bowDamageTaken = 0;
  private int shotsTaken = 0;
  private int shotsHit = 0;

  private final double teamKD;
  private final double teamBowAcc;

  public TeamStats(Collection<PlayerStats> playerStats) {

    for (PlayerStats stats : playerStats) {
      teamKills += stats.getKills();
      teamDeaths += stats.getDeaths();
      damageDone += stats.getDamageDone();
      damageTaken += stats.getDamageTaken();
      bowDamage += stats.getBowDamage();
      bowDamageTaken += stats.getBowDamageTaken();
      shotsTaken += stats.getShotsTaken();
      shotsHit += stats.getShotsHit();
    }

    teamKD = teamDeaths == 0 ? teamKills : teamKills / (double) teamDeaths;
    teamBowAcc = shotsTaken == 0 ? Double.NaN : shotsHit / (shotsTaken / (double) 100);
  }

  @Override
  public Number getStat(StatType.Builtin type) {
    return switch (type) {
      case KILLS -> teamKills;
      case DEATHS -> teamDeaths;
      case KILL_DEATH_RATIO -> teamKD;
      case DAMAGE -> damageDone;
      default -> Double.NaN;
    };
  }
}

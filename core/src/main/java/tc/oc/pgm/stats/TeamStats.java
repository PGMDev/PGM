package tc.oc.pgm.stats;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

// Holds calculated total stats for a single team
public class TeamStats {

  private int teamKills = 0;
  private int teamDeaths = 0;
  private double damageDone = 0;
  private double damageTaken = 0;
  private double bowDamage = 0;
  private double bowDamageTaken = 0;
  private int shotsTaken = 0;
  private int shotsHit = 0;

  private double teamKD;
  private double teamBowAcc;

  public TeamStats(Competitor team, StatsMatchModule statsModule) {

    for (MatchPlayer teamPlayer : team.getPlayers()) {
      PlayerStats stats = statsModule.getPlayerStat(teamPlayer.getId());
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

  public int getTeamKills() {
    return teamKills;
  }

  public int getTeamDeaths() {
    return teamDeaths;
  }

  public double getDamageDone() {
    return damageDone;
  }

  public double getDamageTaken() {
    return damageTaken;
  }

  public double getBowDamage() {
    return bowDamage;
  }

  public double getBowDamageTaken() {
    return bowDamageTaken;
  }

  public int getShotsTaken() {
    return shotsTaken;
  }

  public int getShotsHit() {
    return shotsHit;
  }

  public double getTeamKD() {
    return teamKD;
  }

  public double getTeamBowAcc() {
    return teamBowAcc;
  }
}

package tc.oc.pgm.ghostsquadron;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Effect;
import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.classes.ClassMatchModule;

public class GhostSquadronTask implements Runnable {
  public GhostSquadronTask(
      Match match, GhostSquadronMatchModule matchModule, ClassMatchModule classMatchModule) {
    this.match = checkNotNull(match, "match");
    this.matchModule = checkNotNull(matchModule, "ghost squadron match module");
    this.classMatchModule = checkNotNull(classMatchModule, "class match module");
  }

  @Override
  public void run() {
    if (this.matchModule.trackerClass != null) {
      for (UUID userId : this.classMatchModule.getClassMembers(this.matchModule.trackerClass)) {
        MatchPlayer player = this.match.getPlayer(userId);
        if (player == null) continue;

        MatchPlayer closestEnemy = player;
        double closestRadiusSq = Double.MAX_VALUE;

        for (MatchPlayer enemy : this.matchModule.getMatch().getParticipants()) {
          if (enemy.getParty() == player.getParty()) continue;

          double radiusSq =
              enemy.getBukkit().getLocation().distanceSquared(player.getBukkit().getLocation());
          if (radiusSq < closestRadiusSq) {
            closestEnemy = enemy;
            closestRadiusSq = radiusSq;
          }
        }

        player.getBukkit().setCompassTarget(closestEnemy.getBukkit().getLocation());
      }
    }

    for (Map.Entry<Location, UUID> entry : this.matchModule.landmines.entrySet()) {
      MatchPlayer player = this.match.getPlayer(entry.getValue());
      Location loc = entry.getKey();
      if (player == null) continue;

      for (MatchPlayer enemy : this.match.getPlayers()) {
        enemy.getBukkit().playEffect(loc.clone().add(0, .7, 0), Effect.VILLAGER_THUNDERCLOUD, null);
      }
    }

    if (this.matchModule.spiderClass != null) {
      for (UUID userId : this.classMatchModule.getClassMembers(this.matchModule.spiderClass)) {
        MatchPlayer player = this.match.getPlayer(userId);
        if (player == null) continue;

        for (MatchPlayer enemy : this.matchModule.getMatch().getParticipants()) {
          if (enemy.getParty() == player.getParty()) continue;
          if (enemy.getBukkit().getLocation().distanceSquared(player.getBukkit().getLocation())
              < GhostSquadron.SPIDER_SENSE_RADIUS_SQ) {
            this.matchModule.spideySense(player);
            break;
          }
        }
      }
    }
  }

  final Match match;
  final GhostSquadronMatchModule matchModule;
  final ClassMatchModule classMatchModule;
}

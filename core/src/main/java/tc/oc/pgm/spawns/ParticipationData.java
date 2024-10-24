package tc.oc.pgm.spawns;

import java.time.Duration;
import tc.oc.pgm.api.Config.TimePenalty;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.TimeUtils;

class ParticipationData {
  private Team lastTeam;
  private long lastLeaveTick;
  private boolean wasFull;
  private boolean wasStacked;
  private int rejoins;

  public void trackLeave(Match match, Competitor competitor) {
    if (competitor instanceof Team team) {
      if (lastTeam == team) rejoins++;
      lastTeam = team;
      wasFull = team.getSize() >= team.getMaxPlayers() - 1;
      wasStacked = team.isStacked();
    } else {
      lastTeam = null;
      wasFull = match.getParticipants().size() >= match.getMaxPlayers();
      wasStacked = false;
    }
    lastLeaveTick = match.getTick().tick;
  }

  public long getJoinTick(Team newTeam) {
    TimePenalty penalty = getPenalty(newTeam);
    Duration timeOff = getDuration(penalty);
    if (penalty == TimePenalty.REJOIN_MULTIPLIER) {
      timeOff = TimeUtils.min(timeOff.multipliedBy(rejoins), getDuration(TimePenalty.REJOIN_MAX));
    }
    return lastLeaveTick + TimeUtils.toTicks(timeOff);
  }

  static Duration getDuration(TimePenalty penalty) {
    return penalty == null ? Duration.ZERO : PGM.get().getConfiguration().getTimePenalty(penalty);
  }

  private TimePenalty getPenalty(Team newTeam) {
    if (lastTeam == null || newTeam == null) { // FFA
      return wasFull ? TimePenalty.FFA_FULL_REJOIN : null;
    }
    // Left and rejoined to get the team stacked
    if (newTeam.isStacked()) return TimePenalty.STACKED;

    if (lastTeam == newTeam) { // Team rejoin
      // Making space for someone else to join
      if (wasFull && newTeam.getSize() >= newTeam.getMaxPlayers()) return TimePenalty.FULL_REJOIN;
      // Rejoin spam for resources
      return TimePenalty.REJOIN_MULTIPLIER;
    } else {
      // Explicitly forgive switching to un-stack
      if (wasStacked && !newTeam.isStacked()) return null;

      return TimePenalty.STACKED;
    }
  }
}

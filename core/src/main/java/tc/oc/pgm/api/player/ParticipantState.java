package tc.oc.pgm.api.player;

import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.party.Competitor;

/** A {@link MatchPlayerState} that exclusively represents a {@link Competitor}. */
public interface ParticipantState extends MatchPlayerState {

  @Override
  @NotNull
  Competitor getParty();
}

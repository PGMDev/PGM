package tc.oc.pgm.command.injectors;

import tc.oc.pgm.api.match.Match;

public final class MatchProvider extends MatchObjectProvider<Match> {

  @Override
  protected Match get(Match match) {
    return match;
  }
}

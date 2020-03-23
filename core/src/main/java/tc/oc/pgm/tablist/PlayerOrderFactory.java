package tc.oc.pgm.tablist;

import tc.oc.pgm.api.player.MatchPlayer;

@FunctionalInterface
public interface PlayerOrderFactory {
  PlayerOrder getOrder(MatchPlayer viewer);
}

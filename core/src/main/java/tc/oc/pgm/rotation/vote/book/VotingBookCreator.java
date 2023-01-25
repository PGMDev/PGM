package tc.oc.pgm.rotation.vote.book;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.player.MatchPlayer;

public interface VotingBookCreator {

  Component getMapBookComponent(MatchPlayer viewer, MapInfo map, boolean voted);
}

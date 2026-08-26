package tc.oc.pgm.rotation.vote.book;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.player.MatchPlayer;

public final class PollIntegration {

  private static final AtomicReference<VotingBookCreator> BOOK_CREATOR =
      new AtomicReference<>(new VotingBookCreatorImpl());
  private static final AtomicReference<PollExtensions> POLL_EXTENSIONS =
      new AtomicReference<>(new PollExtensionsImpl());

  public static void setVotingBookCreator(VotingBookCreator bookCreator) {
    BOOK_CREATOR.set(assertNotNull(bookCreator));
  }

  public static VotingBookCreator getVotingBookCreator() {
    return BOOK_CREATOR.get();
  }

  public static PollExtensions getPollExtensions() {
    return POLL_EXTENSIONS.get();
  }

  public static void setPollExtensions(PollExtensions pollExtensions) {
    POLL_EXTENSIONS.set(pollExtensions);
  }

  public static Component getMapBookComponent(MatchPlayer viewer, MapInfo map, boolean voted) {
    return BOOK_CREATOR.get().getMapBookComponent(viewer, map, voted);
  }

  public static Component getMapBookFooter(MatchPlayer viewer) {
    return BOOK_CREATOR.get().getMapBookFooter(viewer);
  }

  public static int getVotingPower(UUID player) {
    return POLL_EXTENSIONS.get().getVotingPower(player);
  }

  public static boolean isSpecialMap(MapInfo map) {
    return POLL_EXTENSIONS.get().isSpecialMap(map);
  }
}

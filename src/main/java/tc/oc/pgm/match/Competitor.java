package tc.oc.pgm.match;

import java.util.Set;
import java.util.UUID;
import org.bukkit.scoreboard.NameTagVisibility;

/** Something that can exclusively win a {@link Match} */
public interface Competitor extends Party {

  /** Return an ID that is constant and unique for the entire match */
  String getId();

  NameTagVisibility getNameTagVisibility();

  /** All players who have ever been in this party after match commitment */
  Set<UUID> getPastPlayers();

  /** Called when the match is committed (see {@link Match#commit()}) */
  void commit();
}

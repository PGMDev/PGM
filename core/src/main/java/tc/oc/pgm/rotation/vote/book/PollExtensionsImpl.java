package tc.oc.pgm.rotation.vote.book;

import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;

public class PollExtensionsImpl implements PollExtensions {
  @Override
  public int getVotingPower(UUID uuid) {
    var player = Bukkit.getPlayer(uuid);
    if (player == null) return 1;
    var cfg = PGM.get().getConfiguration();
    if (!cfg.allowExtraVotes()) return 1;
    for (int i = cfg.getMaxExtraVotes(); i > 1; i--) {
      if (player.hasPermission(Permissions.EXTRA_VOTE + "." + i)) return i;
    }
    return player.hasPermission(Permissions.EXTRA_VOTE) ? 2 : 1;
  }

  @Override
  public boolean isSpecialMap(MapInfo map) {
    return PGM.get().getMapOrder() instanceof MapPoolManager manager
        && manager.getVoteOptions().getCustomVoteMaps().contains(map);
  }
}

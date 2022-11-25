package tc.oc.pgm.api.player;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Helper interface to resolve a {@link MatchPlayer}, {@link MatchPlayerState}, or {@link
 * ParticipantState} from {@link Bukkit} primitives.
 */
public interface MatchPlayerResolver {

  @Nullable
  MatchPlayer getPlayer(@Nullable Player player);

  @Nullable
  default MatchPlayer getPlayer(@Nullable Entity player) {
    return player instanceof Player ? getPlayer((Player) player) : null;
  }

  @Nullable
  default MatchPlayer getPlayer(@Nullable MatchPlayerState state) {
    return state == null ? null : getPlayer(state.getId());
  }

  @Nullable
  default MatchPlayer getPlayer(@Nullable UUID playerId) {
    return playerId == null ? null : getPlayer(Bukkit.getPlayer(playerId));
  }

  @Nullable
  default MatchPlayerState getPlayerState(@Nullable UUID playerId) {
    if (playerId == null) return null;
    MatchPlayer matchPlayer = getPlayer(playerId);
    return matchPlayer == null ? null : matchPlayer.getState();
  }

  @Nullable
  default MatchPlayerState getPlayerState(@Nullable Player player) {
    if (player == null) return null;
    MatchPlayer matchPlayer = getPlayer(player);
    return matchPlayer == null ? null : matchPlayer.getState();
  }

  @Nullable
  default MatchPlayerState getPlayerState(@Nullable Entity entity) {
    return entity instanceof Player ? getPlayerState((Player) entity) : null;
  }

  @Nullable
  default ParticipantState getParticipantState(@Nullable Player player) {
    if (player == null) return null;
    MatchPlayer matchPlayer = getPlayer(player);
    return matchPlayer == null ? null : matchPlayer.getParticipantState();
  }

  @Nullable
  default ParticipantState getParticipantState(@Nullable Entity entity) {
    return entity instanceof Player ? getParticipantState((Player) entity) : null;
  }

  @Nullable
  default ParticipantState getParticipantState(@Nullable UUID playerId) {
    if (playerId == null) return null;
    MatchPlayer matchPlayer = getPlayer(playerId);
    return matchPlayer == null ? null : matchPlayer.getParticipantState();
  }

  @Nullable
  default MatchPlayer getParticipant(@Nullable Player bukkit) {
    final MatchPlayer player = getPlayer(bukkit);
    if (player != null && player.isParticipating()) {
      return player;
    }
    return null;
  }

  @Nullable
  default MatchPlayer getParticipant(@Nullable Entity entity) {
    return entity instanceof Player ? getParticipant((Player) entity) : null;
  }
}

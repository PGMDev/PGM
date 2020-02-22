package tc.oc.util.bukkit.identity;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Captures aspects of a player's state that affect how they are identified to other players. A
 * player assumes a new Identity whenever they change their nickname, or their visibility (when we
 * have /vanish). Two Identities are equal if and only if they are seen as the same player, in the
 * same state, by all possible viewers.
 *
 * <p>It should never be assumed that an {@link Identity} is the player's *current* identity. The
 * value of representing identities with a concrete object is that they can be stored and displayed
 * even after the player who they belong to has assumed a different identity.
 */
public interface Identity {

  /** The ID of the player who used this identity */
  UUID getPlayerId();

  Player getPlayer();

  /** The (real) name of the player who used this identity */
  String getRealName();

  /** The nickname used for this identity, or null if the player's real name is used */
  @Nullable
  String getNickname();

  /** Does this identity belong to the player with the given ID? */
  boolean isSelf(UUID user);

  /** Does this identity belong to the given player? */
  boolean isSelf(Player player);

  /** Does this identity belong to the given sender? */
  boolean isSelf(CommandSender sender);

  /** Is the owner of this identity current online and using this identity? */
  boolean isCurrent();

  /** Is this identity currently in use by the given player? */
  boolean isCurrent(Player player);

  /** The name of this identity as seen by the given viewer */
  String getName(CommandSender viewer);

  /**
   * The CURRENT online state of this identity as seen by the given viewer (NOT the state at the
   * time the identity was created)
   */
  boolean isOnline(CommandSender viewer);

  /**
   * The CURRENT living/dead state of this identity as seen by the given viewer (NOT the state at
   * the time the identity was created)
   */
  boolean isDead(CommandSender viewer);

  /** Is this identity friends with the given viewer, and the viewer is allowed to know this? */
  boolean isFriend(CommandSender viewer);

  /** Should the true owner of this identity be revealed to the given viewer? */
  boolean isRevealed(CommandSender viewer);
}

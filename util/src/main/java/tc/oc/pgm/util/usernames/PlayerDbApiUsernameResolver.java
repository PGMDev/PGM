package tc.oc.pgm.util.usernames;

/**
 * Resolves using PlayerDb
 *
 * @link <a href="https://github.com/nodecraft/playerdb">Github</a>
 * @link <a href="https://playerdb.co">Website</a>
 * @link <a
 *     href="https://playerdb.co/api/player/minecraft/853c80ef-3c37-49fd-aa49-938b674adae6">Example
 *     response</a>
 */
public class PlayerDbApiUsernameResolver extends ApiUsernameResolver {
  public PlayerDbApiUsernameResolver() {
    super(
        "playerdb",
        "https://playerdb.co/api/player/minecraft/{uuid}",
        false,
        "data.player.username");
  }
}

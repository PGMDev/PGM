package tc.oc.pgm.util.usernames;

import com.google.gson.JsonObject;

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
    super("https://playerdb.co/api/player/minecraft/{uuid}");
  }

  @Override
  protected String getUsername(JsonObject response) {
    var success = response.get("success");
    if (success == null || !success.getAsBoolean()) return null;
    var data = response.getAsJsonObject("data");
    if (data == null) return null;
    var player = data.getAsJsonObject("player");
    if (player == null) return null;
    var username = player.get("username");
    return username == null ? null : username.getAsString();
  }
}

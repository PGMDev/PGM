package tc.oc.pgm.util.usernames;

import com.google.gson.JsonObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Resolves using mojang's official minecraft api
 *
 * @link <a href="https://minecraft.wiki/w/Mojang_API">Mojang API on minecraft.wiki</a>
 * @link <a
 *     href="https://api.minecraftservices.com/minecraft/profile/lookup/853c80ef-3c37-49fd-aa49-938b674adae6">Example
 *     response</a>
 */
public class MojangApiUsernameResolver extends ApiUsernameResolver {
  // Given the known rate-limit, pin everything to a single thread and make resolving wait
  private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

  public MojangApiUsernameResolver() {
    super("https://api.minecraftservices.com/minecraft/profile/lookup/{uuid}");
  }

  @Override
  protected Executor getExecutor() {
    return EXECUTOR;
  }

  @Override
  protected String getUsername(JsonObject response) {
    var name = response.get("name");
    return name == null ? null : name.getAsString();
  }
}

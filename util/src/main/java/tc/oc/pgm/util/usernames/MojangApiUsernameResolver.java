package tc.oc.pgm.util.usernames;

/**
 * Resolves using mojang's official minecraft api
 *
 * @link <a href="https://minecraft.wiki/w/Mojang_API">Mojang API on minecraft.wiki</a>
 * @link <a
 *     href="https://api.minecraftservices.com/minecraft/profile/lookup/853c80ef-3c37-49fd-aa49-938b674adae6">Example
 *     response</a>
 */
public class MojangApiUsernameResolver extends ApiUsernameResolver {
  public MojangApiUsernameResolver() {
    super(
        "mojang",
        "https://api.minecraftservices.com/minecraft/profile/lookup/{uuid}",
        true,
        "name");
  }
}

package tc.oc.pgm.util.usernames;

/**
 * Resolves using electroid's mojang-api
 *
 * @link <a href="https://github.com/Electroid/mojang-api">Github</a>
 * @link <a
 *     href="https://api.ashcon.app/mojang/v2/user/853c80ef-3c37-49fd-aa49-938b674adae6">Example
 *     response</a>
 */
public class ElectroidApiUsernameResolver extends ApiUsernameResolver {

  public ElectroidApiUsernameResolver() {
    super("electroid", "https://api.ashcon.app/mojang/v2/user/{uuid}", false, "username");
  }
}

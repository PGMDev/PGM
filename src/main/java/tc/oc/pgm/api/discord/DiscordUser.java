package tc.oc.pgm.api.discord;

import java.util.Collection;

public interface DiscordUser extends DiscordId {

  String getTag();

  Collection<String> getRoles();
}

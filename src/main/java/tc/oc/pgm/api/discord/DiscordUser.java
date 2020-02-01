package tc.oc.pgm.api.discord;

import jdk.internal.jline.internal.Nullable;

public interface DiscordUser {
    // Gets the Discord username of the user, or null if not registered
    @Nullable
    String getUsername();

    // Sets the Discord username
    void setUsername(String username);

    // Gets verification token for a user, cached or un-cached
    String getToken();
}
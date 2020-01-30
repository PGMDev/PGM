package tc.oc.pgm.discord;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import tc.oc.pgm.Config;

public class DiscordClient {

  private static JDA jda;
  public static final Cache<UUID, String> TOKENS_CACHE =
      CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

  public static void initialize() throws InterruptedException, LoginException {
    jda = new JDABuilder(Config.Discord.token()).build();
    jda.awaitReady();
  }

  public static User getUserFromTag(String tag) {
    return jda.getUserByTag(tag);
  }

  public static void sendMessage(User user, String message) {
    user.openPrivateChannel()
        .submit()
        .thenCompose(channel -> channel.sendMessage(message).submit())
        .whenComplete(
            (m, error) -> {
              if (error != null) error.printStackTrace();
            });
  }
}

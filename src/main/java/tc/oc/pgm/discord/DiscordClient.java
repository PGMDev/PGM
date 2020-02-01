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

  private JDA jda;
  public final Cache<UUID, String> TOKENS_CACHE =
      CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

  public DiscordClient() throws InterruptedException, LoginException {
    this.jda = new JDABuilder(Config.Discord.token()).build();
    this.jda.awaitReady();
  }

  public User getUserFromTag(String tag) {
    return jda.getUserByTag(tag);
  }

  public void sendMessage(User user, String message) {
    user.openPrivateChannel()
        .submit()
        .thenCompose(channel -> channel.sendMessage(message).submit())
        .whenComplete(
            (m, error) -> {
              if (error != null) error.printStackTrace();
            });
  }
}

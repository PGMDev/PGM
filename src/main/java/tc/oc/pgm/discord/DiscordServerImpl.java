package tc.oc.pgm.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.datastore.OneTimePin;
import tc.oc.pgm.api.discord.DiscordId;
import tc.oc.pgm.api.discord.DiscordServer;
import tc.oc.pgm.api.discord.DiscordUser;

public class DiscordServerImpl extends ListenerAdapter implements Listener, DiscordServer {

  private final Logger logger;
  private final JDABuilder builder;
  private JDA client;

  public DiscordServerImpl(Logger logger) {
    this.logger = checkNotNull(logger); // In-game logger
    this.builder =
        new JDABuilder()
            .setGuildSubscriptionsEnabled(false)
            .setDisabledCacheFlags(EnumSet.allOf(CacheFlag.class))
            .addEventListeners(this);
  }

  @Override
  public void connect(String token) {
    if (client != null && Objects.equals(client.getToken(), checkNotNull(token))) return;

    CompletableFuture.runAsync(
        () -> {
          synchronized (builder) {
            builder.setToken(token);
            try {
              client = builder.build();
            } catch (LoginException e) {
              logger.log(Level.WARNING, "Unable to connect to Discord", e);
            }
          }
        });
  }

  private static final Pattern COMMAND = Pattern.compile("/verify ([a-zA-Z0-9_]{1,16}) (.*)");

  public boolean requestVerification(long discordId, String username, String code) {
    final Player player = Bukkit.getPlayerExact(username);
    if (player == null) return false;

    final OneTimePin pin = PGM.get().getDatastore().getOneTimePin(null, code);
    if (pin == null || !pin.isValid() || !Objects.equals(pin.getId(), player.getUniqueId()))
      return false;

    final DiscordId discordUser = PGM.get().getDatastore().getDiscordId(pin.getId());
    discordUser.setSnowflake(discordId);
    pin.markAsUsed();
    return true;
  }

  @Override
  public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
    final String message = event.getMessage().getContentStripped();
    final Matcher matcher = COMMAND.matcher(message);

    if (matcher.matches()
        && requestVerification(event.getAuthor().getIdLong(), matcher.group(1), matcher.group(2))) {
      event.getMessage().addReaction("✅").submit();
    }
  }

  @Override
  public DiscordUser getBot() {
    if (client == null) return null;
    return new DiscordUser() {
      @Override
      public String getTag() {
        return client.getSelfUser().getAsTag();
      }

      @Override
      public Collection<String> getRoles() {
        return null;
      }

      @Override
      public UUID getId() {
        return null;
      }

      @Override
      public Long getSnowflake() {
        return client.getSelfUser().getIdLong();
      }

      @Override
      public void setSnowflake(@Nullable Long snowflake) {}
    };
  }

  @Override
  public DiscordUser getUser(long id) {
    return new DiscordUser() {
      @Override
      public String getTag() {
        return client.retrieveUserById(id).submit().join().getAsTag();
      }

      @Override
      public Collection<String> getRoles() {
        return null;
      }

      @Override
      public UUID getId() {
        return null;
      }

      @Nullable
      @Override
      public Long getSnowflake() {
        return id;
      }

      @Override
      public void setSnowflake(@Nullable Long snowflake) {}
    };
  }
}

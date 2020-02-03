package tc.oc.pgm.discord;

import java.util.EnumSet;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.datastore.Datastore;
import tc.oc.pgm.api.datastore.OneTimePin;
import tc.oc.pgm.api.discord.DiscordId;

public class DiscordManager extends ListenerAdapter implements Listener {

  private final JDA client;
  private final Datastore datastore;

  public DiscordManager(String token) throws LoginException {
    datastore = PGM.get().getDatastore();
    client =
        new JDABuilder()
            .setToken(token)
            .setAutoReconnect(true)
            .setBulkDeleteSplittingEnabled(true)
            .setRequestTimeoutRetry(true)
            .setEnableShutdownHook(true)
            .setGuildSubscriptionsEnabled(true)
            .setDisabledCacheFlags(EnumSet.allOf(CacheFlag.class))
            .addEventListeners(this)
            .build();
  }

  @Override
  public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
    final String message = event.getMessage().getContentStripped();
    System.out.println("RECEIVED: '" + message + "'"); // DEBUG
    if (!message.startsWith("/verify ")) return;

    final String code = message.substring(message.indexOf(" ") + 1).trim();
    System.out.println("CODE: '" + code + "'"); // DEBUG
    final OneTimePin pin = datastore.getOneTimePin(null, code);
    if (pin == null || !pin.isValid()) return;

    final DiscordId user = datastore.getDiscordId(pin.getId());
    user.setSnowflake(event.getAuthor().getIdLong());
    event.getMessage().addReaction(user.getSnowflake() == null ? "-1" : "+1").submit();
  }
}

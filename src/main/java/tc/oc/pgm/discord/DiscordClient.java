package tc.oc.pgm.discord;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import tc.oc.pgm.Config;

public class DiscordClient extends ListenerAdapter {

  public static void initialize() throws InterruptedException, LoginException {
    System.out.println("Test");
    JDA jda = new JDABuilder(Config.Discord.token()).addEventListeners(new DiscordClient()).build();
    System.out.println(jda == null);
  }

  @Override
  public void onReady(ReadyEvent e) {
    System.out.println("Connected to Discord");
  }
}

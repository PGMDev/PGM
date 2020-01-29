package tc.oc.pgm.discord;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import tc.oc.pgm.Config;

public class DiscordClient {

  public static void initialize() {
    try {
      JDA jda = new JDABuilder(Config.Discord.token()).build();
    } catch (LoginException e) {
      System.out.println("Discord can't login with the token provided");
    }
  }
}

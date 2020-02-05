package tc.oc.pgm.api.discord;

public interface DiscordServer {

  void connect(String token);

  DiscordUser getBot();

  DiscordUser getUser(long id);
}

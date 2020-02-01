package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.argument.ArgumentException;
import java.util.Map;
import java.util.UUID;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.discord.DiscordClient;

public class DiscordCommands {

  @Command(
      aliases = {"link"},
      desc = "Links a Discord user to your Minecraft account",
      usage = "[username]")
  public static void link(CommandSender sender, MatchPlayer player, String DiscordTag)
      throws ArgumentException {
    if (player == null)
      throw new ArgumentException(AllTranslations.get().translate("command.onlyPlayers", sender));
    DiscordClient discordClient = PGM.get().getDiscordClient();
    User user = discordClient.getUserFromTag(DiscordTag);
    // TODO: Check if user has linked an account
    UUID uuid = player.getId();
    if (discordClient.TOKENS_CACHE.getIfPresent(uuid) == null) {
      if (user != null) {
        String token = UUID.randomUUID().toString().replace("-", "");
        discordClient.sendMessage(
            user,
            AllTranslations.get()
                .translate(
                    "discord.verify",
                    sender,
                    sender.getName(),
                    "/discord verify " + token)); // FIXME: Check if DM got delivered, if not Error.
        discordClient.TOKENS_CACHE.put(uuid, token);
        sender.sendMessage(
            ChatColor.GREEN
                + AllTranslations.get().translate("discord.sentmessage", sender, DiscordTag));
      } else {
        throw new ArgumentException(
            AllTranslations.get().translate("discord.notfound", sender, DiscordTag));
      }
    } else {
      // TODO: Send error with the timeleft till next command
    }
  }

  @Command(
      aliases = {"verify"},
      desc = "Verify your account",
      usage = "[token]")
  public static void verify(CommandSender sender, MatchPlayer player, String token)
      throws ArgumentException {
    if (player == null)
      throw new ArgumentException(AllTranslations.get().translate("command.onlyPlayers", sender));
    DiscordClient discordClient = PGM.get().getDiscordClient();
    if (token != null) {
      boolean exists =
          discordClient.TOKENS_CACHE.asMap().entrySet().stream()
              .filter(entry -> token.equals(entry.getValue()))
              .map(Map.Entry::getKey)
              .findFirst()
              .isPresent();
      if (exists) {
        discordClient.TOKENS_CACHE.asMap().entrySet().stream()
            .filter(entry -> token.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .findFirst()
            .get();
        // Link Accounts in Datastore
        sender.sendMessage(
            ChatColor.GREEN + AllTranslations.get().translate("discord.linked", sender));
      }
    }
  }

  @Command(
      aliases = {"unlink"},
      desc = "Unlinks your accounts",
      usage = "[token]")
  public static void unlink(CommandSender sender, MatchPlayer player, String DiscordTAG)
      throws ArgumentException {
    if (player == null)
      throw new ArgumentException(AllTranslations.get().translate("command.onlyPlayers", sender));
  }
}

package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.argument.ArgumentException;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.discord.DiscordClient;

public class DiscordCommands {

  @Command(
      aliases = {"discord"},
      desc = "Discord Integration Commands",
      usage = "[action] <option>")
  public static void discord(
      CommandSender sender, MatchPlayer player, String action, @Nullable String option)
      throws ArgumentException {
    if (player == null)
      throw new ArgumentException(AllTranslations.get().translate("command.onlyPlayers", sender));
    switch (action) {
      case "link":
        User user = DiscordClient.getUserFromTag(option);
        // TODO: Check if user has linked an account
        UUID uuid = player.getId();
        if (DiscordClient.TOKENS_CACHE.getIfPresent(uuid) == null) {
          if (user != null) {
            String token = UUID.randomUUID().toString().replace("-", "");
            DiscordClient.sendMessage(
                user,
                AllTranslations.get()
                    .translate(
                        "discord.verify",
                        sender,
                        "/discord verify "
                            + token)); // FIXME: Check if DM got delivered, if not Error.
            DiscordClient.TOKENS_CACHE.put(uuid, token);
            sender.sendMessage(
                ChatColor.GREEN + AllTranslations.get().translate("discord.sentmessage", sender));
          } else {
            throw new ArgumentException(
                AllTranslations.get().translate("discord.notfound", sender, option));
          }
        } else {
          // TODO: Send error with the timeleft till next command
        }
        break;
      case "unlink":

      case "verify":
        if (option != null) {
          boolean exists =
              DiscordClient.TOKENS_CACHE.asMap().entrySet().stream()
                  .filter(entry -> option.equals(entry.getValue()))
                  .map(Map.Entry::getKey)
                  .findFirst()
                  .isPresent();
          if (exists) {
            DiscordClient.TOKENS_CACHE.asMap().entrySet().stream()
                .filter(entry -> option.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .get();
            // Link Accounts in Datastore
            sender.sendMessage(
                ChatColor.GREEN + AllTranslations.get().translate("discord.linked", sender));
          } else {
            // Invalid token
          }
        } else {
          // Token Not Set
        }
        break;
      default:
        // invalid argument
    }
  }
}

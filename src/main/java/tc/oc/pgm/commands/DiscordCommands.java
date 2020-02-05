package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import net.md_5.bungee.api.ChatColor;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.datastore.Datastore;
import tc.oc.pgm.api.datastore.OneTimePin;
import tc.oc.pgm.api.discord.DiscordId;
import tc.oc.pgm.api.discord.DiscordServer;
import tc.oc.pgm.api.discord.DiscordUser;
import tc.oc.pgm.api.player.MatchPlayer;

public class DiscordCommands {

  @Command(
      aliases = {"discord"},
      desc = "Discord command")
  public void discord(MatchPlayer player, Datastore datastore, DiscordServer server) {
    final DiscordId id = datastore.getDiscordId(player.getId());
    if (id.getSnowflake() == null) {
      final OneTimePin pin = datastore.getOneTimePin(player.getId(), null);
      player.sendMessage(
          new PersonalizedTranslatable(
              "discord.verify.request",
              new PersonalizedText(server.getBot().getTag(), ChatColor.DARK_AQUA),
              new PersonalizedText(
                  "/verify " + player.getBukkit().getName() + " " + pin.getPin(), ChatColor.AQUA)));
    } else {
      final DiscordUser user = server.getUser(id.getSnowflake());
      player.sendMessage(
          new PersonalizedTranslatable(
              "discord.verify.status", new PersonalizedText(user.getTag(), ChatColor.DARK_AQUA)));
    }
  }
}

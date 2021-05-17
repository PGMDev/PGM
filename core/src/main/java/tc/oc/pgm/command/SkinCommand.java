package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Skin;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

public final class SkinCommand {

  // TODO: this is very buggy, either fix or remove

  @Command(
      aliases = {"info"},
      desc = "Dump the encoded data for a player's skin",
      usage = "[player]",
      perms = Permissions.SKIN)
  public void info(Audience sender, Player target) throws CommandException {
    Skin skin = target.getSkin();
    sender.sendMessage(text("Textures:", NamedTextColor.BLUE).append(text(skin.getData())));
    sender.sendMessage(text("Signature:", NamedTextColor.BLUE).append(text(skin.getSignature())));
  }

  @Command(
      aliases = {"reset"},
      desc = "Reset a player's skin to their real one",
      usage = "[player]",
      perms = Permissions.SKIN)
  public void reset(Audience sender, Match match, Player target) throws CommandException {
    MatchPlayer player = match.getPlayer(target);
    target.setSkin(null);
    player.resetVisibility();
    sender.sendMessage(text().append(text("Reset the skin of ")).append(player.getName()));
  }

  @Command(
      aliases = {"clone"},
      desc = "Clone one player's skin to another",
      usage = "<source> [target]",
      flags = "u",
      perms = Permissions.SKIN)
  public void clone(
      Audience sender, Match match, Player source, Player target, @Switch('u') boolean unsigned)
      throws CommandException {
    MatchPlayer sourcePlayer = match.getPlayer(source);
    MatchPlayer targetPlayer = match.getPlayer(target);
    Skin skin = source.getSkin();
    if (unsigned) {
      skin = new Skin(skin.getData(), null);
    }

    target.setSkin(skin);
    targetPlayer.resetVisibility();
    sender.sendMessage(
        text()
            .append(text("Cloned "))
            .append(sourcePlayer.getName())
            .append(text("'s skin to ").append(targetPlayer.getName())));
  }

  @Command(
      aliases = {"none"},
      desc = "Clear a player's skin, making them steve/alex",
      usage = "[player]",
      perms = Permissions.SKIN)
  public void none(Audience sender, Match match, Player target) throws CommandException {
    MatchPlayer player = match.getPlayer(target);
    target.setSkin(Skin.EMPTY);
    player.resetVisibility();
    sender.sendMessage(text().append(text("Cleared the skin of ")).append(player.getName()));
  }
}

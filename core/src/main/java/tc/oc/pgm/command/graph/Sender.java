package tc.oc.pgm.command.graph;

import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

public class Sender implements Audience {
  protected final CommandSender sender;
  protected final Match match;
  protected final MatchPlayer player;
  protected final Audience audience;

  public Sender(CommandSender sender, Match match, MatchPlayer player) {
    this.sender = sender;
    this.match = match;
    this.player = player;
    this.audience = player != null ? player : Audience.get(sender);
  }

  public CommandSender getSender() {
    return sender;
  }

  public Match getMatch() {
    return match;
  }

  public @Nullable MatchPlayer getPlayer() {
    return player;
  }

  public boolean hasPermission(String permission) {
    return sender.hasPermission(permission);
  }

  public boolean hasPermission(Permission permission) {
    return sender.hasPermission(permission);
  }

  @Override
  public @NotNull Audience audience() {
    return audience;
  }


  public static class Player extends Sender {

    public Player(org.bukkit.entity.Player sender, Match match, @NotNull MatchPlayer player) {
      super(sender, match, player);
    }

    public org.bukkit.entity.Player getSender() {
      return (org.bukkit.entity.Player) sender;
    }

    public @NotNull MatchPlayer getPlayer() {
      return player;
    }

    public UUID getId() {
      return player.getId();
    }
  }
}

package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class MapDevCommand {

  @Command(
      aliases = {"velocity", "vel"},
      desc = "Apply a velocity to a player",
      perms = Permissions.MAPDEV)
  public void velocity(
      Audience viewer, CommandSender sender, Player target, Double x, Double y, Double z)
      throws CommandException {
    boolean self = (sender instanceof Player) && ((Player) sender).equals(target);
    Vector velocity = new Vector(x, y, z);
    TextComponent.Builder message =
        TextComponent.builder().append(TranslatableComponent.of("command.mapdev.velocity"));
    if (!self) {
      message.append(TextComponent.space()).append(PlayerComponent.of(target, NameStyle.FANCY));
    }
    message
        .append(" (")
        .append(String.format("%.2f", velocity.getX()), getVelocityColor(velocity.getX()))
        .append(", ")
        .append(String.format("%.2f", velocity.getY()), getVelocityColor(velocity.getY()))
        .append(", ")
        .append(String.format("%.2f", velocity.getZ()), getVelocityColor(velocity.getZ()))
        .append(")")
        .color(TextColor.GRAY)
        .build();
    viewer.sendMessage(message.build());
    target.setVelocity(velocity);
  }

  private TextColor getVelocityColor(Double value) {
    return value > 0 ? TextColor.GREEN : TextColor.RED;
  }
}

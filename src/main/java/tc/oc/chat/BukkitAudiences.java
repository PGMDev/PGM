package tc.oc.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class BukkitAudiences {
  private BukkitAudiences() {}

  public static Audience getAudience(CommandSender sender) {
    if (sender instanceof Player) {
      return new PlayerAudience((Player) sender);
    } else if (sender instanceof ConsoleCommandSender) {
      return new CommandSenderAudience(sender.getServer().getConsoleSender());
    } else {
      return new CommandSenderAudience(sender);
    }
  }
}

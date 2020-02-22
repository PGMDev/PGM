package tc.oc.pgm.api.chat;

import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentRenderers;

public class CommandSenderAudience implements Audience {

  protected final CommandSender sender;

  public CommandSenderAudience(CommandSender sender) {
    this.sender = sender;
  }

  protected CommandSender getCommandSender() {
    return sender;
  }

  @Override
  public void sendMessage(String message) {
    getCommandSender().sendMessage(message);
  }

  @Override
  public void sendMessage(Component message) {
    ComponentRenderers.send(getCommandSender(), message);
  }

  @Override
  public void sendHotbarMessage(Component message) {}

  @Override
  public void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {}

  @Override
  public void playSound(Sound sound) {}
}

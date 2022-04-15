package tc.oc.pgm.util.audience;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.text.TextTranslations;

public class ConsoleAudience implements Audience {

  @Override
  public void sendMessage(
      @NotNull final Identity source,
      @NotNull final Component message,
      @NotNull final MessageType type) {
    ConsoleCommandSender sender = Bukkit.getConsoleSender();
    sender.sendMessage(TextTranslations.translate(message, sender));
  }
}

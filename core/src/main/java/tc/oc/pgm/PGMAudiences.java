package tc.oc.pgm;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;

public class PGMAudiences {

  public final BukkitAudiences PROVIDER;

  PGMAudiences(Plugin plugin) {
    this.PROVIDER = BukkitAudiences.create(plugin);
  }

  public final Audience console() {
    return PROVIDER.console();
  }

  /** Sends a warning message to an audience */
  public static void sendWarning(Component message, Audience audience) {
    audience.sendMessage(
        Component.text(" \u26a0 ", NamedTextColor.YELLOW)
            .append(message.colorIfAbsent(NamedTextColor.RED)));
    audience.playSound(Sound.sound(Key.key("note.bass"), Sound.Source.MASTER, 1f, 0.75f));
  }
}

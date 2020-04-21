package tc.oc.pgm.util.component;

import app.ashcon.sportpaper.api.text.PersonalizedComponent;
import java.util.Collection;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComponentRenderers {

  public static void send(Player viewer, Component component) {
    viewer.sendMessage(component.render(viewer));
  }

  public static void send(Player viewer, Collection<? extends Component> components) {
    for (PersonalizedComponent component : components) {
      viewer.sendMessage(component.render(viewer));
    }
  }

  // TODO: when CommandSender has component support...
  public static void send(CommandSender viewer, Component component) {
    if (viewer instanceof Player) {
      send((Player) viewer, component);
    } else {
      viewer.sendMessage(component.render(viewer).toPlainText());
    }
  }

  public static void send(CommandSender viewer, Collection<? extends Component> components) {
    if (viewer instanceof Player) {
      send((Player) viewer, components);
    } else {
      for (PersonalizedComponent component : components) {
        viewer.sendMessage(component.render(viewer).toPlainText());
      }
    }
  }

  /** Serialize to a JSON String */
  public static String toJson(PersonalizedComponent component, CommandSender viewer) {
    return ComponentSerializer.toString(component.render(viewer));
  }

  /**
   * Convert to legacy text. Just calls {@link BaseComponent#toLegacyText()} for now, but we may
   * have to replace that at some point due to its dependence on the parent field.
   */
  public static String toLegacyText(PersonalizedComponent component, CommandSender viewer) {
    return component.render(viewer).toLegacyText();
  }
}

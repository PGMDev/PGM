package tc.oc.pgm.util.text;

import java.text.MessageFormat;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentRenderer extends TranslatableComponentRenderer<Pointered> {

  public static final ComponentRenderer RENDERER = new ComponentRenderer();

  private ComponentRenderer() {}

  @Override
  public @NotNull Component render(@NotNull Component component, @NotNull Pointered context) {
    if (component instanceof RenderableComponent) {
      CommandSender sender =
          context
              .get(Identity.UUID)
              .<CommandSender>map(Bukkit::getPlayer)
              .orElse(Bukkit.getConsoleSender());
      component = ((RenderableComponent) component).render(sender);
    }

    return super.render(component, context);
  }

  @Override
  protected @Nullable MessageFormat translate(@NotNull String key, @NotNull Pointered context) {
    return GlobalTranslator.translator().translate(key, TextTranslations.getLocale(context));
  }
}

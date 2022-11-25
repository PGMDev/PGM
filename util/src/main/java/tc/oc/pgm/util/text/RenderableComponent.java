package tc.oc.pgm.util.text;

import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.ScopedComponent;
import net.kyori.adventure.text.format.Style;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public interface RenderableComponent extends ScopedComponent<RenderableComponent> {

  Component render(CommandSender viewer);

  @Override
  default @NotNull RenderableComponent children(@NotNull List<? extends ComponentLike> children) {
    return this;
  }

  @Override
  default @NotNull RenderableComponent style(final @NotNull Style style) {
    return this;
  }

  @Override
  default @Unmodifiable @NotNull List<Component> children() {
    return Collections.emptyList();
  }

  @Override
  default @NotNull Style style() {
    return Style.empty();
  }
}

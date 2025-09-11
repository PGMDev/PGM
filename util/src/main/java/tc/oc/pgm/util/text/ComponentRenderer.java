package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.VirtualComponent;
import net.kyori.adventure.text.VirtualComponentRenderer;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentRenderer extends TranslatableComponentRenderer<Pointered> {

  public static final ComponentRenderer RENDERER = new ComponentRenderer();
  private final Map<Class<?>, Function<Pointered, Object>> contextFactory = Map.of(
      CommandSender.class,
          p -> p.get(Identity.UUID)
              .<CommandSender>map(Bukkit::getPlayer)
              .orElse(Bukkit.getConsoleSender()),
      Player.class, p -> p.get(Identity.UUID).map(Bukkit::getPlayer).orElse(null));

  private ComponentRenderer() {}

  @Override
  protected @NotNull Component renderVirtual(
      @NotNull VirtualComponent vc, @NotNull Pointered pointer) {
    var factory = contextFactory.get(vc.contextType());
    if (factory == null)
      throw new UnsupportedOperationException("Context type not supported: " + vc.contextType());

    Component rendered = doRender(vc, factory.apply(pointer));
    var style = vc.style();
    if (!style.isEmpty()) rendered = rendered.style(rendered.style().merge(style));
    return rendered;
  }

  private static <T> Component doRender(VirtualComponent vc, T context) {
    if (context == null) return text(vc.renderer().fallbackString());

    if (!vc.contextType().isInstance(context))
      throw new IllegalArgumentException("Wrong context type for virtual component: " + vc);

    //noinspection unchecked
    return ((VirtualComponentRenderer<? super T>) vc.renderer()).apply(context).asComponent();
  }

  @Override
  protected @Nullable MessageFormat translate(@NotNull String key, @NotNull Pointered context) {
    return GlobalTranslator.translator().translate(key, TextTranslations.getLocale(context));
  }

  /**
   * Reimplementation of TranslatableComponentRenderer#renderTranslatable to allow virtual
   * components rendering.<br>
   * See <a href="https://github.com/KyoriPowered/adventure/issues/1299">Adventure#1299</a>.
   */
  @Override
  protected @NotNull Component renderTranslatable(
      @NotNull TranslatableComponent component, final @NotNull Pointered context) {
    final List<TranslationArgument> arguments = component.arguments();

    if (!arguments.isEmpty()) {
      final List<TranslationArgument> translatedArguments = new ArrayList<>(arguments);
      for (int i = 0; i < arguments.size(); i++) {
        if (arguments.get(i).value() instanceof Component c)
          translatedArguments.set(i, TranslationArgument.component(this.render(c, context)));
      }

      component = component.toBuilder().arguments(translatedArguments).build();
    }

    return this.renderTranslatableInner(component, context);
  }
}

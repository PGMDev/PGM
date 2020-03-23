package tc.oc.util.bukkit.translations2;

import java.util.Locale;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.renderer.FriendlyComponentRenderer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Provides {@link Component} translations based on {@link Locale}.
 *
 * @see #getComponent(Component, Locale)
 */
public class ComponentProvider extends MessageFormatProvider {

  private final FriendlyComponentRenderer<Locale> componentRenderer;

  public ComponentProvider(@Nullable String resourceName, Locale defaultLocale) {
    super(resourceName, defaultLocale);
    this.componentRenderer = FriendlyComponentRenderer.from(this::getFormat);
  }

  /**
   * Translate a {@link Component} based on a {@link Locale}.
   *
   * @param message The message to translate.
   * @param locale The locale to use.
   * @return A rendered message in the locale.
   */
  // TODO: Cache renders that occurs in the same tick
  public Component getComponent(Component message, Locale locale) {
    try {
      return componentRenderer.render(message, locale);
    } catch (Throwable t) {
      // Should never happen, but safe-guard since messages are a critical path
      return message;
    }
  }

  public String getLegacy(Component message, Locale locale) {
    return LegacyComponentSerializer.INSTANCE.serialize(getComponent(message, locale));
  }
}

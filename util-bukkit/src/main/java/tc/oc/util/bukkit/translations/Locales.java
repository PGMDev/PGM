package tc.oc.util.bukkit.translations;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Locales {

  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  /**
   * Attempts to retrieve the best locale for the specified {@link Player}. If a null player is
   * given, the default locale is returned.
   *
   * @param sender to get locale from
   * @return {@link Locale}
   */
  public static Locale getLocale(CommandSender sender) {
    try {
      return checkNotNull(sender).getLocale();
    } catch (IllegalArgumentException | NullPointerException e) {
      return Locales.DEFAULT_LOCALE;
    }
  }

  /**
   * Attempts to retrieve the best locale for the specified {@link Player}. If a null player is
   * given, the default locale is returned.
   *
   * @param player to get locale from
   * @return {@link Locale}
   */
  public static Locale getLocale(Player player) {
    return getLocale((CommandSender) player);
  }

  /**
   * Returns a list of all the supported {@link Locale}s in a {@link Collection} of {@link
   * TranslationProvider}s
   *
   * @param providers to check locale support
   * @return {@link Set} of supported {@link Locale}
   */
  public static Set<Locale> getSupportedLocales(
      Collection<tc.oc.util.bukkit.translations.provider.TranslationProvider> providers) {
    Set<Locale> locales = new HashSet<>();
    locales.add(DEFAULT_LOCALE);
    locales.add(new Locale("af", "ZA"));
    for (Locale locale : Locale.getAvailableLocales()) {
      for (tc.oc.util.bukkit.translations.provider.TranslationProvider provider : providers) {
        try {
          provider.getTranslations(locale);
          locales.add(locale);
          break;
        } catch (MissingResourceException e) {
          // ignore
        }
      }
    }
    return locales;
  }
}

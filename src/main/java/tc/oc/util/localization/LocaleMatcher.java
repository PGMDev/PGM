package tc.oc.util.localization;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

@SuppressWarnings("UnstableApiUsage")
public class LocaleMatcher {
  private final Locale defaultLocale;
  private final Set<Locale> supportedLocales;
  private final LoadingCache<Locale, Locale> closestMatches =
      CacheBuilder.newBuilder()
          .expireAfterAccess(10, TimeUnit.MINUTES)
          .build(
              new CacheLoader<Locale, Locale>() {
                @Override
                public Locale load(@Nonnull Locale locale) {
                  return internalClosestMatchFor(locale);
                }
              });

  public static final int UNRELATED_LANGUAGE = -1;

  public LocaleMatcher(Locale defaultLocale, Set<Locale> supportedLocales) {
    this.defaultLocale = Preconditions.checkNotNull(defaultLocale, "no default locale available");
    this.supportedLocales = Preconditions.checkNotNull(supportedLocales, "no supported locales");
  }

  public Locale closestMatchFor(Locale target) {
    try {
      return closestMatches.get(target);
    } catch (ExecutionException e) {
      return defaultLocale;
    }
  }

  private Locale internalClosestMatchFor(Locale target) {
    Locale bestMatch = this.defaultLocale;
    int bestMatchScore = UNRELATED_LANGUAGE;

    for (Locale potentialBestMatch : this.supportedLocales) {
      int currentComparison = this.compareLocales(target, potentialBestMatch);

      if (currentComparison > bestMatchScore) {
        bestMatch = potentialBestMatch;
        bestMatchScore = currentComparison;
      }
    }

    return bestMatch;
  }

  public int compareLocales(Locale l1, Locale l2) {
    if (l1.getLanguage().equals(l2.getLanguage())) {
      if (l1.getCountry().equals(l2.getCountry())) {
        if (l1.getVariant().equals(l2.getVariant())) {
          return 2;
        } else {
          return 1;
        }
      } else {
        return 0;
      }
    } else {
      return UNRELATED_LANGUAGE;
    }
  }
}

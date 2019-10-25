package tc.oc.util.localization;

import com.google.common.collect.ForwardingMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * {@link Map} adapter that passes keys through a {@link LocaleMatcher} before looking them up in
 * the delegated map.
 */
public class LocaleMap<V> extends ForwardingMap<Locale, V> {

  private final LocaleMatcher matcher;
  private final Map<Locale, V> map;

  public LocaleMap(Map<Locale, V> map) {
    this.map = map;
    this.matcher = new LocaleMatcher(Locales.DEFAULT_LOCALE, map.keySet());
  }

  public LocaleMap() {
    this(new HashMap<Locale, V>());
  }

  @Override
  protected Map<Locale, V> delegate() {
    return map;
  }

  @Override
  public V get(@Nullable Object key) {
    return key instanceof Locale ? super.get(matcher.closestMatchFor((Locale) key)) : null;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return key instanceof Locale && super.containsKey(matcher.closestMatchFor((Locale) key));
  }
}

package tc.oc.pgm.util.translation;

import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.text.TextTranslations;

public class Translation {

  private String message;
  private Map<String, String> translated;

  public Translation(String message) {
    this.message = message;
    this.translated = Maps.newHashMap();
  }

  public String getMessage() {
    return message;
  }

  public String getMessage(Player player) {
    return getMessage(getPlayerLanguageCode(player));
  }

  public String getMessage(String language) {
    return translated.getOrDefault(language, message);
  }

  public Map<String, String> getTranslated() {
    return translated;
  }

  public void addTranslated(String language, String translatedMessage) {
    this.translated.put(language, translatedMessage);
  }

  public boolean isTranslated(String language) {
    return translated.containsKey(language);
  }

  public static String getPlayerLanguageCode(Player player) {
    Locale locale = TextTranslations.getNearestLocale(player.getLocale());
    return locale.getLanguage();
  }
}

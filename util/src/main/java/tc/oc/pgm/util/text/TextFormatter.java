package tc.oc.pgm.util.text;

import com.google.common.collect.Collections2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.named.Named;

/** A helper for formatting {@link Component}s. */
public final class TextFormatter {
  private TextFormatter() {}

  /**
   * Gets a list of text.
   *
   * @param texts A collection of text.
   * @param color The color of the list separators.
   * @return A text list.
   */
  public static Component list(Collection<? extends Component> texts, TextColor color) {
    final List<? extends Component> textList =
        texts instanceof List ? (List) texts : new ArrayList<>(texts);
    switch (textList.size()) {
      case 0:
        return TextComponent.empty();
      case 1:
        return textList.get(0);
      case 2:
        return TranslatableComponent.of("misc.list.pair", color, textList);
      default:
        final Iterator<? extends Component> textIterator = textList.iterator();
        Component a =
            TranslatableComponent.of(
                "misc.list.start", color, textIterator.next(), textIterator.next());
        Component b = textIterator.next();
        while (textIterator.hasNext()) {
          a = TranslatableComponent.of("misc.list.middle", color, a, b);
          b = textIterator.next();
        }
        return TranslatableComponent.of("misc.list.end", color, a, b);
    }
  }

  /**
   * Gets a list of names.
   *
   * @see #list(Collection, TextColor)
   * @param names A collection of names.
   * @param style The style of each name.
   * @param color The color of the list separators.
   * @return A name list.
   */
  public static Component nameList(
      Collection<? extends Named> names, NameStyle style, TextColor color) {
    return list(Collections2.transform(names, name -> name.getName(style)), color);
  }
}

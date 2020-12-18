package tc.oc.pgm.util.text;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.LegacyFormatUtils;
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
        return Component.empty();
      case 1:
        return textList.get(0);
      case 2:
        return Component.translatable("misc.list.pair", color, textList);
      default:
        final Iterator<? extends Component> textIterator = textList.iterator();
        Component a =
            Component.translatable(
                "misc.list.start", color, textIterator.next(), textIterator.next());
        Component b = textIterator.next();
        while (textIterator.hasNext()) {
          a = Component.translatable("misc.list.middle", color, a, b);
          b = textIterator.next();
        }
        return Component.translatable("misc.list.end", color, a, b);
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

  /**
   * Appends formatted page number to end of provided text
   *
   * @param text Component to apply page to
   * @param page The current page number
   * @param pages The total number of pages
   * @param mainColor The color of the text
   * @param pageColor The color of the page numbers
   * @param simple Whether to include 'Page' in wording
   * @return A message with page information appended.
   */
  public static Component paginate(
      Component text,
      int page,
      int pages,
      TextColor mainColor,
      TextColor pageColor,
      boolean simple) {
    return Component.text()
        .append(text)
        .append(Component.space())
        .append(Component.text("(", mainColor))
        .append(
            Component.translatable(
                    simple ? "command.simplePageHeader" : "command.pageHeader",
                    NamedTextColor.DARK_AQUA)
                .args(Component.text(page, pageColor), Component.text(pages, pageColor)))
        .append(Component.text(")", mainColor))
        .build();
  }

  /**
   * Formats the provided text with a header that spans the chat window.
   *
   * @param sender Who is viewing the list
   * @param text The text to format
   * @param lineColor Color of the line
   * @param width length of the line
   * @return A formatted header
   */
  public static Component horizontalLineHeading(
      CommandSender sender, Component text, TextColor lineColor, int width) {
    return horizontalLineHeading(sender, text, lineColor, TextDecoration.STRIKETHROUGH, width);
  }

  public static Component horizontalLineHeading(
      CommandSender sender,
      Component text,
      TextColor lineColor,
      TextDecoration decoration,
      int width) {
    text =
        Component.text().append(Component.space()).append(text).append(Component.space()).build();
    int textWidth = LegacyFormatUtils.pixelWidth(TextTranslations.translateLegacy(text, sender));
    int spaceCount =
        Math.max(0, ((width - textWidth) / 2 + 1) / (LegacyFormatUtils.SPACE_PIXEL_WIDTH + 1));
    String line = Strings.repeat(" ", spaceCount);
    return Component.text()
        .append(Component.text(line, lineColor, decoration))
        .append(text)
        .append(Component.text(line, lineColor, decoration))
        .build();
  }

  public static Component horizontalLineHeading(
      CommandSender sender, Component text, TextColor lineColor) {
    return horizontalLineHeading(sender, text, lineColor, LegacyFormatUtils.MAX_CHAT_WIDTH);
  }

  /*
   * Convert ChatColor -> TextColor
   */
  public static TextColor convert(Enum<?> color) {
    TextColor textColor = NamedTextColor.WHITE;
    try {
      textColor = NamedTextColor.NAMES.value(color.name().toLowerCase());
    } catch (IllegalArgumentException e) {
      // If not found use default
    }
    return textColor;
  }

  public static TextFormat convertFormat(Enum<?> color) {
    TextFormat textColor = NamedTextColor.WHITE;
    try {
      textColor = NamedTextColor.NAMES.value(color.name().toLowerCase());
    } catch (IllegalArgumentException e) {
      // If not found use default
      if ((color instanceof ChatColor) && convertDecoration((ChatColor) color) != null) {
        textColor = convertDecoration((ChatColor) color);
      }
    }
    return textColor;
  }

  @Nullable
  public static TextDecoration convertDecoration(ChatColor color) {
    switch (color) {
      case BOLD:
        return TextDecoration.BOLD;
      case ITALIC:
        return TextDecoration.ITALIC;
      case MAGIC:
        return TextDecoration.OBFUSCATED;
      case STRIKETHROUGH:
        return TextDecoration.STRIKETHROUGH;
      case UNDERLINE:
        return TextDecoration.UNDERLINED;
      default:
        return null;
    }
  }
}

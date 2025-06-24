package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.named.Named;

/** A helper for formatting {@link Component}s. */
public final class TextFormatter {

  public static final int GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH = 55;
  public static final int MAX_CHAT_WIDTH = 300;

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
        texts instanceof List ? (List<? extends Component>) texts : new ArrayList<>(texts);
    return switch (textList.size()) {
      case 0 -> empty();
      case 1 -> textList.getFirst().colorIfAbsent(color);
      case 2 -> translatable("misc.list.pair", color, textList);
      default -> {
        var it = textList.iterator();
        Component a = translatable("misc.list.start", color, it.next(), it.next());
        Component b = it.next();
        while (it.hasNext()) {
          a = translatable("misc.list.middle", color, a, b);
          b = it.next();
        }
        yield translatable("misc.list.end", color, a, b);
      }
    };
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
    return text()
        .append(text)
        .append(space())
        .append(text("(", mainColor))
        .append(translatable(
            simple ? "command.simplePageHeader" : "command.pageHeader",
            mainColor,
            text(page, pageColor),
            text(pages, pageColor)))
        .append(text(")", mainColor))
        .build();
  }

  /**
   * Return a horizontal line spanning the width of the chat window
   *
   * @param lineColor color of the line
   * @param width width of the line in pixels
   * @return the line as a string
   */
  public static Component horizontalLine(TextColor lineColor, int width) {
    return text(
        Strings.repeat(" ", (width + 1) / (LegacyFormatUtils.SPACE_PIXEL_WIDTH + 1)),
        lineColor,
        TextDecoration.STRIKETHROUGH);
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
      CommandSender sender, ComponentLike text, TextColor lineColor, int width) {
    return horizontalLineHeading(sender, text, lineColor, TextDecoration.STRIKETHROUGH, width);
  }

  public static Component horizontalLineHeading(
      CommandSender sender,
      ComponentLike text,
      TextColor lineColor,
      TextDecoration decoration,
      int width) {
    text = text().append(space()).append(text).append(space()).build();
    int textWidth =
        LegacyFormatUtils.pixelWidth(TextTranslations.translateLegacy(text.asComponent(), sender));
    int spaceCount =
        Math.max(0, ((width - textWidth) / 2 + 1) / (LegacyFormatUtils.SPACE_PIXEL_WIDTH + 1));
    String line = Strings.repeat(" ", spaceCount);
    return text()
        .append(text(line, lineColor, decoration))
        .append(text)
        .append(text(line, lineColor, decoration))
        .build();
  }

  public static Component horizontalLineHeading(
      CommandSender sender, ComponentLike text, TextColor lineColor) {
    return horizontalLineHeading(sender, text, lineColor, LegacyFormatUtils.MAX_CHAT_WIDTH);
  }

  /*
   * Convert ChatColor -> TextColor
   */
  public static NamedTextColor convert(ChatColor color) {
    return NamedTextColor.namedColor(BukkitUtils.colorOf(color).asRGB());
  }

  public static TextFormat convertFormat(ChatColor color) {
    TextFormat decoration = convertDecoration(color);
    return decoration != null ? decoration : convert(color);
  }

  @Nullable
  public static TextDecoration convertDecoration(ChatColor color) {
    return switch (color) {
      case BOLD -> TextDecoration.BOLD;
      case ITALIC -> TextDecoration.ITALIC;
      case MAGIC -> TextDecoration.OBFUSCATED;
      case STRIKETHROUGH -> TextDecoration.STRIKETHROUGH;
      case UNDERLINE -> TextDecoration.UNDERLINED;
      default -> null;
    };
  }
}

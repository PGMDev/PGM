package tc.oc.pgm.util;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;

/**
 * These utilities should no longer be used, instead use {@link tc.oc.pgm.util.text.TextFormatter}
 *
 * <p>TODO: Determine if any of these would be useful and move to {@link
 * tc.oc.pgm.util.text.TextFormatter}
 */
public final class LegacyFormatUtils {

  public static final int GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH = 55;
  public static final int MAX_CHAT_WIDTH = 300;

  /**
   * Get the pixel width of the given character in the Minecraft font, excluding the space between
   * characters and drop shadow. Handles alphanumerics and most common english punctuation.
   */
  public static int pixelWidth(char c) {
    if (Character.isUpperCase(c)) {
      return c == 'I' ? 3 : 5;
    } else if (Character.isDigit(c)) {
      return 5;
    } else if (Character.isLowerCase(c)) {
      switch (c) {
        case 'i':
          return 1;

        case 'l':
          return 2;

        case 't':
          return 3;

        case 'f':
        case 'k':
          return 4;

        default:
          return 5;
      }
    } else {
      switch (c) {
        case '!':
        case '.':
        case ',':
        case ';':
        case ':':
        case '|':
          return 1;

        case '\'':
          return 2;

        case '[':
        case ']':
        case ' ':
          return 3;

        case '*':
        case '(':
        case ')':
        case '{':
        case '}':
        case '<':
        case '>':
          return 4;

        case '@':
          return 6;

        default:
          return 5;
      }
    }
  }

  /** A complete text formatting state, used by Parser */
  private static class Format {
    public boolean obfuscated = false;
    public boolean bold = false;
    public boolean strikethrough = false;
    public boolean underline = false;
    public boolean italic = false;
    public ChatColor color;

    private Format(
        boolean obfuscated,
        boolean bold,
        boolean strikethrough,
        boolean underline,
        boolean italic,
        ChatColor color) {
      this.obfuscated = obfuscated;
      this.bold = bold;
      this.strikethrough = strikethrough;
      this.underline = underline;
      this.italic = italic;
      this.color = color;
    }

    private Format(Format format) {
      this(
          format.obfuscated,
          format.bold,
          format.strikethrough,
          format.underline,
          format.italic,
          format.color);
    }

    private Format() {}

    @Override
    public String toString() {
      String str = "";

      if (this.color != null) {
        str += this.color;
      }
      if (this.obfuscated) {
        str += ChatColor.MAGIC;
      }
      if (this.bold) {
        str += ChatColor.BOLD;
      }
      if (this.strikethrough) {
        str += ChatColor.STRIKETHROUGH;
      }
      if (this.underline) {
        str += ChatColor.UNDERLINE;
      }
      if (this.italic) {
        str += ChatColor.ITALIC;
      }

      return str;
    }
  }

  /**
   * Holds the state while parsing formatted text, while calculating the pixel width at various
   * points.
   */
  private static class Parser {
    public final String text;

    public boolean formatting = false; // last char was §
    public int chars = 0; // consumed char count
    public int pixels = 0; // total pixel width of consumed chars
    public Format format; // formatting state at current pos

    public int charsVisible = 0; // char count at last visible char
    public int pixelsVisible = 0; // pixel width at last visible char

    public int charsWord = 0; // char count at last word ending
    public int pixelsWord = 0; // pixel width at last word ending
    public Format wordFormat; // formatting state at last word ending

    private Parser(String text) {
      this.text = text;
      this.format = new Format();
      this.wordFormat = new Format();
    }

    public boolean atEnd() {
      return this.chars == this.text.length();
    }

    /** Return the pixels that will be added to width if the given char is consumed next */
    public int getAdvance(char c) {
      if (c == ChatColor.COLOR_CHAR && this.chars < this.text.length() - 1) {
        return 0;
      } else {
        int width = pixelWidth(c);

        if (this.format.bold) {
          // Bold chars are one pixel wider
          width += 1;
        }

        if (this.chars != 0) {
          // If not the first char, add the gap between chars
          width += 1;
        }

        return width;
      }
    }

    /** Return the number of pixels that will be added to width by the next char */
    public int nextAdvance() {
      return this.getAdvance(this.text.charAt(this.chars));
    }

    /** Consume a character */
    public void advance() {
      char c = this.text.charAt(this.chars++);

      if (this.formatting) {
        // Previous char was §, so this char will always be hidden
        switch (c) {
          case 'k':
            this.format.obfuscated = true;
            break;

          case 'l':
            this.format.bold = true;
            break;

          case 'm':
            this.format.strikethrough = true;
            break;

          case 'n':
            this.format.underline = true;
            break;

          case 'o':
            this.format.italic = true;
            break;

          default:
            this.format.color = ChatColor.getByChar(c);
            this.format.obfuscated = false;
            this.format.bold = false;
            this.format.strikethrough = false;
            this.format.underline = false;
            this.format.italic = false;
            break;

          case 'r':
            this.format.obfuscated = false;
            this.format.bold = false;
            this.format.strikethrough = false;
            this.format.underline = false;
            this.format.italic = false;
            this.format.color = null;
            break;
        }

        this.formatting = false;
      } else {
        if (c == ChatColor.COLOR_CHAR && this.chars != this.text.length() - 1) {
          // If we encounter a § and it's not the last char in the string,
          // switch to the format state
          this.formatting = true;
        } else {
          // Otherwise, this char will be visible, so update the position
          // pointer and increment the width
          this.charsVisible = this.chars;
          this.pixelsVisible = this.pixels;

          if (this.chars == this.text.length()
              || Character.isWhitespace(this.text.charAt(this.chars))) {
            this.charsWord = this.chars;
            this.pixelsWord = this.pixels;
            this.wordFormat = new Format(this.format);
          }

          this.pixels += getAdvance(c);
        }
      }
    }

    /**
     * Consume the maximum characters that will increase the width by no more than the given amount
     */
    public void advance(int width) {
      int startWidth = this.pixels;

      while (this.pixels + this.nextAdvance() <= startWidth + width) {
        this.advance();

        if (this.atEnd()) {
          return;
        }
      }
    }

    /** Consume the maximum words that will increase the width by no more than the given amount */
    public void advanceWords(int width) {
      int startWidth = this.pixels;

      while (this.pixels + this.nextAdvance() <= startWidth + width) {
        this.advance();

        if (this.atEnd()) {
          return;
        }
      }

      this.chars = this.charsWord;
      this.pixels = this.pixelsWord;
      this.format = new Format(this.wordFormat);
    }
  }

  /**
   * Get the pixel width of the given text in the Minecraft font, excluding the drop shadow.
   * Formatting codes are accounted for, and so is bold text.
   */
  public static int pixelWidth(String text) {
    return pixelWidth(text, false);
  }

  public static int pixelWidth(String text, boolean bold) {
    Parser parser = new Parser(text);
    if (bold) parser.format.bold = true;
    while (!parser.atEnd()) {
      parser.advance();
    }
    return parser.pixels;
  }

  /**
   * Return the length of the longest prefix of the given string that fits within the given pixel
   * width when rendered in the Minecraft font. If the prefix is shorter than the entire string,
   * then it is guaranteed to finish with a visible character, and not any formatting character.
   */
  public static int longestPrefix(String text, int maxWidth) {
    int pos = 0;
    Parser parser = new Parser(text);

    while (parser.pixels < maxWidth) {
      if (parser.atEnd()) {
        return text.length();
      }
      pos = parser.charsVisible;
      parser.advance();
    }

    return pos;
  }

  /**
   * Return the length of the longest prefix of the given string that ends on a word boundary and
   * fits within the given pixel width when rendered in the Minecraft font. If the prefix is shorter
   * than the entire string, then it is guaranteed to finish with a visible character, and not any
   * formatting character.
   */
  public static int longestWordPrefix(String text, int maxWidth) {
    int pos = 0;
    Parser parser = new Parser(text);

    while (parser.pixels < maxWidth) {
      if (parser.atEnd()) {
        return text.length();
      }
      pos = parser.charsWord;
      parser.advance();
    }

    return pos;
  }

  public static final int SPACE_PIXEL_WIDTH = pixelWidth(' ');

  /**
   * Return a horizontal line spanning the width of the chat window
   *
   * @param lineColor color of the line
   * @param width width of the line in pixels
   * @return the line as a string
   */
  public static String horizontalLine(ChatColor lineColor, int width) {
    return lineColor.toString()
        + ChatColor.STRIKETHROUGH
        + Strings.repeat(" ", (width + 1) / (SPACE_PIXEL_WIDTH + 1));
  }

  /**
   * Return some text centered in a horizontal line spanning the chat window
   *
   * @param text the text to center in the line (can contain formatting codes)
   * @param lineColor the color of the line
   * @param width width of the line in pixels
   * @return the heading as a string
   */
  public static String horizontalLineHeading(String text, ChatColor lineColor, int width) {
    text = ChatColor.RESET + " " + text + ChatColor.RESET + " ";
    int textWidth = pixelWidth(text);
    int spaceCount = Math.max(0, ((width - textWidth) / 2 + 1) / (SPACE_PIXEL_WIDTH + 1));
    return lineColor.toString()
        + ChatColor.STRIKETHROUGH
        + Strings.repeat(" ", spaceCount)
        + text
        + lineColor.toString()
        + ChatColor.STRIKETHROUGH
        + Strings.repeat(" ", spaceCount);
  }

  public static String horizontalLineHeading(String text, ChatColor lineColor) {
    return horizontalLineHeading(text, lineColor, MAX_CHAT_WIDTH);
  }

  public static List<String> wordWrap(String text, int width) {
    ArrayList<String> lines = new ArrayList<>();
    return wordWrap(text, width, lines);
  }

  /**
   * Word-wrap the given text to the given pixel width. Output is appended to the given String list,
   * which is also returned. Formatting codes are correctly handled and propagated across lines.
   *
   * @param text to wrap
   * @param width pixel width of the chat window
   * @param lines list of lines to append the wrapped text to
   * @return lines
   */
  public static List<String> wordWrap(String text, int width, List<String> lines) {
    Parser parser = new Parser(text);
    Format format = null;
    int lineStart = 0;

    while (!parser.atEnd()) {
      parser.advanceWords(width);
      if (parser.chars == lineStart) {
        parser.advance(width);
        if (parser.chars == lineStart) {
          parser.advance();
        }
      }

      if (format == null) {
        lines.add(text.substring(lineStart, parser.chars));
      } else {
        lines.add(format.toString() + text.substring(lineStart, parser.chars));
      }

      lineStart = parser.chars;
      format = new Format(parser.format);
    }

    return lines;
  }

  public static String dashedChatMessage(String message, String dash, String dashPrefix) {
    return dashedChatMessage(message, dash, dashPrefix, null);
  }

  public static String dashedChatMessage(
      String message, String dash, String dashPrefix, String messagePrefix) {
    message = " " + message + " ";
    int dashCount =
        (GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - ChatColor.stripColor(message).length() - 2)
            / (dash.length() * 2);
    String dashes = dashCount >= 0 ? Strings.repeat(dash, dashCount) : "";

    StringBuffer builder = new StringBuffer();
    if (dashCount > 0) {
      builder.append(dashPrefix).append(dashes).append(ChatColor.RESET);
    }

    if (messagePrefix != null) {
      builder.append(messagePrefix);
    }

    builder.append(message);

    if (dashCount > 0) {
      builder.append(ChatColor.RESET).append(dashPrefix).append(dashes);
    }

    return builder.toString();
  }

  /**
   * Underlines a specific string maintaining colors.
   *
   * @param str The string to underline.
   * @return The underlined string.
   */
  public static String underline(String str) {
    return applyFormat(ChatColor.UNDERLINE, str);
  }

  /**
   * Makes a string bolded, maintaining all colors.
   *
   * @param str The string to be bolded.
   * @return The bolded string.
   */
  public static String bold(String str) {
    return applyFormat(ChatColor.BOLD, str);
  }

  /**
   * Italicizes a string, maintaining all previous colors.
   *
   * @param str The string to be italicized.
   * @return The intaliciezd string.
   */
  public static String italicize(String str) {
    return applyFormat(ChatColor.ITALIC, str);
  }

  /**
   * Strikes out a string, keeping all previous colors.
   *
   * @param str The string to strike out.
   * @return The striked-out string.
   */
  public static String strikethrough(String str) {
    return applyFormat(ChatColor.STRIKETHROUGH, str);
  }

  /**
   * Replaces all instances of one color in "str" from `from` to `to`.
   *
   * @param str The base string.
   * @param from The base ChatColor.
   * @param to The ChatColor to replace to.
   * @return The replaced String.
   */
  public static String replaceColorCodes(String str, ChatColor from, ChatColor to) {
    return str.replaceAll(from.toString(), to.toString());
  }

  public static boolean isFormat(ChatColor color) {
    switch (color) {
      case BOLD:
      case ITALIC:
      case UNDERLINE:
      case STRIKETHROUGH:
      case MAGIC:
        return true;

      default:
        return false;
    }
  }

  /**
   * Apply formatting to an entire string that may contain other color codes
   *
   * @param format The format to apply
   * @param text The string to apply the format to
   * @return New string with the format applied
   */
  private static String applyFormat(ChatColor format, String text) {
    StringBuilder buffer = new StringBuilder();
    boolean escaped = false; // previous char was the escape char
    boolean needsFormat = true; // format has been reset and needs to be re-applied

    for (int i = 0; i < text.length(); ++i) {
      char c = text.charAt(i);

      if (escaped) {
        escaped = false;
        ChatColor color = ChatColor.getByChar(c);
        needsFormat = color == null || !isFormat(color);
      } else if (c == ChatColor.COLOR_CHAR) {
        escaped = true;
      } else if (needsFormat) {
        needsFormat = false;
        buffer.append(format);
      }

      buffer.append(c);
    }

    return buffer.toString();
  }

  /** Make text really small (only works on numbers right now) */
  public static String tiny(String text) {
    char[] chars = text.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      switch (chars[i]) {
        case '.':
        case ',': // Some locales will use , as the decimal separator
          chars[i] = '\u2024';
          break;
        default:
          chars[i] = (char) (chars[i] - '0' + '\u2080');
          break;
      }
    }
    return String.valueOf(chars);
  }
}

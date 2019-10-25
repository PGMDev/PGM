package tc.oc.util.components;

import static com.google.common.base.Preconditions.checkNotNull;

import app.ashcon.sportpaper.api.text.PersonalizedComponent;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import tc.oc.component.Component;
import tc.oc.component.types.BlankComponent;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;

/** Utils for working with {@link Component}s */
public class Components {

  private Components() {
    // Don't instantiate
  }

  public static BaseComponent[] fromLegacyTextMulti(String legacyText) {
    return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('`', legacyText));
  }

  /** Convert text with legacy formatting codes to a {@link Component} */
  public static Component fromLegacyText(String legacyText) {
    return new PersonalizedText(fromLegacyTextMulti(legacyText));
  }

  public static List<Component> fromBungee(List<BaseComponent> baseComponents) {
    return baseComponents.stream().map(Component::new).collect(Collectors.toList());
  }

  public static List<Component> fromBungee(BaseComponent[] baseComponents) {
    return Arrays.stream(baseComponents).map(Component::new).collect(Collectors.toList());
  }

  private static final Component SPACE = new PersonalizedText(" ");

  public static Component blank() {
    return BlankComponent.INSTANCE;
  }

  public static Component space() {
    return SPACE;
  }

  public static boolean isBlank(@Nullable PersonalizedComponent c) {
    return c == null || c instanceof BlankComponent;
  }

  /** See {@link #format(MessageFormat, List)} */
  public static Component[] format(String format, Component... arguments) {
    return format(new MessageFormat(format), arguments);
  }

  /** See {@link #format(MessageFormat, List)} */
  public static Component[] format(String format, List<Component> arguments) {
    return format(new MessageFormat(format), arguments);
  }

  /** See {@link #format(MessageFormat, List)} */
  public static Component[] format(MessageFormat format, Component... arguments) {
    return format(format, Arrays.asList(arguments));
  }

  /**
   * Render the given {@link MessageFormat} to component form, using the given arguments. This is
   * equivalent to {@link MessageFormat#format} but a component tree is generated, instead of a
   * String, and the arguments are included directly in the tree.
   *
   * <p>To accomplish this, the message is first rendered to a String with placeholder arguments.
   * Then {@link MessageFormat#formatToCharacterIterator} is used to figure out where the arguments
   * appear in the result, and the text between them is spliced out and used to build the component
   * tree along with the actual arguments.
   *
   * @return {@link Component}
   */
  public static Component[] format(MessageFormat format, List<Component> arguments) {
    if (arguments == null || arguments.isEmpty()) {
      return new Component[] {
        new PersonalizedText(format.format(null, new StringBuffer(), null).toString())
      };
    }

    List<Component> parts = new ArrayList<>(arguments.size() * 2 + 1);

    Object[] dummies = new Object[arguments.size()];
    StringBuffer sb = format.format(dummies, new StringBuffer(), null);
    AttributedCharacterIterator iter = format.formatToCharacterIterator(dummies);

    while (iter.getIndex() < iter.getEndIndex()) {
      int end = iter.getRunLimit();
      Integer index = (Integer) iter.getAttribute(MessageFormat.Field.ARGUMENT);
      if (index == null) {
        parts.add(new PersonalizedText(sb.substring(iter.getIndex(), end)));
      } else {
        parts.add(arguments.get(index));
      }
      iter.setIndex(end);
    }

    return parts.toArray(new Component[0]);
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(HoverEvent a, HoverEvent b) {
    return (a == b)
        || (a != null
            && b != null
            && a.getAction() == b.getAction()
            && equals(a.getValue(), b.getValue()));
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(ClickEvent a, ClickEvent b) {
    return (a == b)
        || (a != null
            && b != null
            && a.getAction() == b.getAction()
            && Objects.equals(a.getValue(), b.getValue()));
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(BaseComponent a, BaseComponent b) {
    return (a == b)
        || (a != null
            && b != null
            && Objects.equals(a.isBoldRaw(), b.isBoldRaw())
            && Objects.equals(a.isItalicRaw(), b.isItalicRaw())
            && Objects.equals(a.isObfuscatedRaw(), b.isItalicRaw())
            && Objects.equals(a.isStrikethroughRaw(), b.isItalicRaw())
            && Objects.equals(a.isUnderlinedRaw(), b.isItalicRaw())
            && equals(a.getClickEvent(), b.getClickEvent())
            && equals(a.getHoverEvent(), b.getHoverEvent()));
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(BaseComponent[] a, BaseComponent[] b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a.length != b.length) return false;

    for (int i = 0; i < a.length; i++) {
      if (!equals(a[i], b[i])) return false;
    }

    return true;
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(Component a, Component b) {
    return (a == b)
        || (a != null
            && b != null
            && Objects.equals(a.isBoldRaw(), b.isBoldRaw())
            && Objects.equals(a.isItalicRaw(), b.isItalicRaw())
            && Objects.equals(a.isObfuscatedRaw(), b.isItalicRaw())
            && Objects.equals(a.isStrikethroughRaw(), b.isItalicRaw())
            && Objects.equals(a.isUnderlinedRaw(), b.isItalicRaw())
            && equals(a.getClickEvent(), b.getClickEvent())
            && equals(a.getHoverEvent(), b.getHoverEvent()));
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(Component[] a, Component[] b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a.length != b.length) return false;

    for (int i = 0; i < a.length; i++) {
      if (!equals(a[i], b[i])) return false;
    }

    return true;
  }

  /** Recursively compare the given components for equality */
  public static boolean equals(Collection<Component> a, Collection<Component> b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a.size() != b.size()) return false;

    for (Iterator<Component> ia = a.iterator(), ib = b.iterator(); ia.hasNext() && ib.hasNext(); ) {
      if (!equals(ia.next(), ib.next())) return false;
    }

    return true;
  }

  public static BaseComponent color(BaseComponent c, @Nullable ChatColor color) {
    c.setColor(color);
    return c;
  }

  public static BaseComponent bold(BaseComponent c, @Nullable Boolean yes) {
    c.setBold(yes);
    return c;
  }

  public static BaseComponent italic(BaseComponent c, @Nullable Boolean yes) {
    c.setItalic(yes);
    return c;
  }

  public static BaseComponent underlined(BaseComponent c, @Nullable Boolean yes) {
    c.setUnderlined(yes);
    return c;
  }

  public static Component strikethrough(Component c, @Nullable Boolean yes) {
    c.setStrikethrough(yes);
    return c;
  }

  public static BaseComponent obfuscated(BaseComponent c, @Nullable Boolean yes) {
    c.setObfuscated(yes);
    return c;
  }

  public static BaseComponent clickEvent(BaseComponent c, @Nullable ClickEvent event) {
    c.setClickEvent(event);
    return c;
  }

  public static BaseComponent clickEvent(BaseComponent c, ClickEvent.Action action, String value) {
    c.setClickEvent(new ClickEvent(action, value));
    return c;
  }

  public static BaseComponent hoverEvent(BaseComponent c, @Nullable HoverEvent event) {
    c.setHoverEvent(event);
    return c;
  }

  public static BaseComponent hoverEvent(
      BaseComponent c, HoverEvent.Action action, BaseComponent... values) {
    c.setHoverEvent(new HoverEvent(action, values));
    return c;
  }

  public static BaseComponent extra(BaseComponent c, BaseComponent... extras) {
    for (BaseComponent extra : extras) {
      checkNotNull(extra);
      c.addExtra(extra);
    }
    return c;
  }

  public static Component addFormats(Component component, ChatColor... formats) {
    for (ChatColor format : formats) {
      checkNotNull(format);
      switch (format) {
        case BOLD:
          component.setBold(true);
          break;
        case ITALIC:
          component.setItalic(true);
          break;
        case UNDERLINE:
          component.setUnderlined(true);
          break;
        case STRIKETHROUGH:
          component.setStrikethrough(true);
          break;
        case MAGIC:
          component.setObfuscated(true);
          break;
        case RESET:
          throw new IllegalArgumentException("Cannot add format " + format);
        default:
          component.setColor(format);
          break;
      }
    }
    return component;
  }

  public static Component removeFormats(Component component, ChatColor... formats) {
    for (ChatColor format : formats) {
      checkNotNull(format);
      switch (format) {
        case BOLD:
          component.setBold(false);
          break;
        case ITALIC:
          component.setItalic(false);
          break;
        case UNDERLINE:
          component.setUnderlined(false);
          break;
        case STRIKETHROUGH:
          component.setStrikethrough(false);
          break;
        case MAGIC:
          component.setObfuscated(false);
          break;
        default:
          throw new IllegalArgumentException("Cannot remove format " + format);
      }
    }
    return component;
  }

  public static boolean hasFormat(BaseComponent c) {
    return c.getColorRaw() != null
        || c.isBoldRaw() != null
        || c.isItalicRaw() != null
        || c.isUnderlinedRaw() != null
        || c.isStrikethroughRaw() != null
        || c.isObfuscatedRaw() != null
        || c.getClickEvent() != null
        || c.getHoverEvent() != null;
  }

  public static void copyFormat(BaseComponent from, BaseComponent to) {
    to.setColor(from.getColorRaw());
    to.setBold(from.isBoldRaw());
    to.setItalic(from.isItalicRaw());
    to.setUnderlined(from.isUnderlinedRaw());
    to.setStrikethrough(from.isStrikethroughRaw());
    to.setObfuscated(from.isObfuscatedRaw());
  }

  public static void copyEvents(BaseComponent from, BaseComponent to) {
    to.setClickEvent(from.getClickEvent());
    to.setHoverEvent(from.getHoverEvent());
  }

  public static void copyFormatAndEvents(BaseComponent from, BaseComponent to) {
    copyFormat(from, to);
    copyEvents(from, to);
  }

  public static void softMergeFormat(BaseComponent from, BaseComponent to) {
    if (to.getColorRaw() == null) to.setColor(from.getColorRaw());
    if (to.isBoldRaw() == null) to.setBold(from.isBoldRaw());
    if (to.isItalicRaw() == null) to.setItalic(from.isItalicRaw());
    if (to.isUnderlinedRaw() == null) to.setUnderlined(from.isUnderlinedRaw());
    if (to.isStrikethroughRaw() == null) to.setStrikethrough(from.isStrikethroughRaw());
    if (to.isObfuscatedRaw() == null) to.setObfuscated(from.isObfuscatedRaw());
    if (to.getClickEvent() == null) to.setClickEvent(from.getClickEvent());
    if (to.getHoverEvent() == null) to.setHoverEvent(from.getHoverEvent());
  }

  public static void hardMergeFormat(BaseComponent from, BaseComponent to) {
    if (from.getColorRaw() != null) to.setColor(from.getColorRaw());
    if (from.isBoldRaw() != null) to.setBold(from.isBoldRaw());
    if (from.isItalicRaw() != null) to.setItalic(from.isItalicRaw());
    if (from.isUnderlinedRaw() != null) to.setUnderlined(from.isUnderlinedRaw());
    if (from.isStrikethroughRaw() != null) to.setStrikethrough(from.isStrikethroughRaw());
    if (from.isObfuscatedRaw() != null) to.setObfuscated(from.isObfuscatedRaw());
    if (from.getClickEvent() != null) to.setClickEvent(from.getClickEvent());
    if (from.getHoverEvent() != null) to.setHoverEvent(from.getHoverEvent());
  }

  public static void copyLastFormat(String legacy, Component to) {
    int length = legacy.length();

    // Search backwards from the end as it is faster
    for (int index = length - 1; index > -1; index--) {
      char section = legacy.charAt(index);
      if (section == ChatColor.COLOR_CHAR && index < length - 1) {
        char c = legacy.charAt(index + 1);
        ChatColor color = ChatColor.getByChar(c);

        if (color != null) {
          addFormats(to, color);

          // Once we find a color or reset we can stop searching
          if (color.equals(ChatColor.RESET)) break;
          if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) break;
        }
      }
    }
  }

  public static void addExtra(BaseComponent c, List<BaseComponent> extra) {
    if (extra != null) {
      if (c.getExtra() == null) {
        c.setExtra(extra);
      } else {
        c.getExtra().addAll(extra);
      }
    }
  }

  public static Component shallowCopy(Component original) {
    if (original instanceof PersonalizedText) {
      return shallowCopy(original);
    } else if (original instanceof PersonalizedTranslatable) {
      return shallowCopy((PersonalizedTranslatable) original);
    } else {
      throw new IllegalArgumentException(
          "Don't know how to copy a " + original.getClass().getName());
    }
  }

  public static Component shallowCopy(PersonalizedText original) {
    PersonalizedText copy = new PersonalizedText(original.getText());
    copyFormatAndEvents(
        original.render(Bukkit.getConsoleSender()), copy.render(Bukkit.getConsoleSender()));
    copy.setExtra(original.getExtra());
    return copy;
  }

  public static Component shallowCopy(PersonalizedTranslatable original) {
    PersonalizedTranslatable copy =
        new PersonalizedTranslatable(original.<TranslatableComponent>getComponent().getTranslate());
    copy.<TranslatableComponent>getComponent()
        .setWith(original.<TranslatableComponent>getComponent().getWith());
    copyFormatAndEvents(original.getComponent(), copy.getComponent());
    copy.setExtra(original.getExtra());
    return copy;
  }

  public static Component concat(Component... components) {
    switch (components.length) {
      case 0:
        return new PersonalizedText();
      case 1:
        return components[0];
      default:
        return new PersonalizedText(components);
    }
  }

  public static Component link(String protocol, String host, String path, ChatColor... formats) {
    try {
      return link(new URL(protocol, host, path), formats);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Component link(URL url, ChatColor... formats) {
    try {
      // The encoded form escapes all illegal characters e.g. " " becomes "%20",
      // which is required by the client. The display form leaves the illegal chars
      // in the path, which tends to look nicer.
      final URI uri = url.toURI();
      final String encoded = url.toExternalForm();
      final String display = encoded.replace(uri.getRawPath(), uri.getPath());
      Component c = new PersonalizedText(display).clickEvent(ClickEvent.Action.OPEN_URL, encoded);
      if (formats.length == 0) {
        c.color(ChatColor.BLUE).underlined(true);
      } else {
        c.add(formats);
      }
      return c;
    } catch (URISyntaxException e) {
      return blank();
    }
  }

  public static int pixelWidth(Collection<Component> components, boolean bold) {
    int width = 0;
    for (Component component : components) {
      width += pixelWidth(component);
    }
    return width;
  }

  public static int pixelWidth(Component component, boolean bold) {
    if (component.isBoldRaw() != null) {
      bold = component.isBold();
    }
    int width = 0;
    if (component instanceof PersonalizedText) {
      String text = ((PersonalizedText) component).getText();
      width += ComponentUtils.pixelWidth(text, bold);
    }
    if (component.getExtra() != null) {
      width +=
          pixelWidth(
              component.getExtra().stream().map(Component::new).collect(Collectors.toList()), bold);
    }
    return width;
  }

  public static int pixelWidth(Component component) {
    return pixelWidth(component, false);
  }

  public static List<BaseComponent> transform(List<String> strings) {
    return strings.stream()
        .map(s -> new PersonalizedText(s).render(Bukkit.getConsoleSender()))
        .collect(Collectors.toList());
  }

  public static BaseComponent join(Component delimiter, Collection<Component> elements) {
    Component c = new PersonalizedText();
    boolean first = true;
    for (Component el : elements) {
      if (!first) {
        c.extra(delimiter);
      }
      c.extra(el);
      first = false;
    }
    return c.render(Bukkit.getConsoleSender());
  }
}

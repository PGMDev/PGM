package tc.oc.util.bukkit.component.types;

import com.google.common.base.Strings;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentUtils;
import tc.oc.util.bukkit.component.Components;
import tc.oc.util.bukkit.component.ImmutableComponent;

@Getter
public class PersonalizedHeader extends ImmutableComponent {

  private static final ChatColor DEFAULT_LINE_COLOR = ChatColor.WHITE;
  private static final int DEFAULT_WIDTH = ComponentUtils.MAX_CHAT_WIDTH;

  private final PersonalizedTranslatable content;
  private final ChatColor lineColor;
  private final int width;

  public PersonalizedHeader(PersonalizedTranslatable content) {
    this(content, DEFAULT_LINE_COLOR);
  }

  public PersonalizedHeader(PersonalizedTranslatable content, ChatColor lineColor, int width) {
    super(new TextComponent(content.toLegacyText()));
    this.content = content;
    this.lineColor = lineColor;
    this.width = width;
  }

  public PersonalizedHeader(PersonalizedTranslatable content, ChatColor lineColor) {
    this(content, lineColor, DEFAULT_WIDTH);
  }

  @Override
  public BaseComponent render(CommandSender commandSender) {
    Component content = new PersonalizedText(Components.space(), getContent(), Components.space());
    int contentWidth = Components.pixelWidth(content);
    int lineChars =
        Math.max(0, ((getWidth() - contentWidth) / 2 + 1) / (ComponentUtils.SPACE_PIXEL_WIDTH + 1));
    Component line =
        new PersonalizedText(
            Strings.repeat(" ", lineChars), getLineColor(), ChatColor.STRIKETHROUGH);
    return new PersonalizedText(line, content, line).render();
  }
}

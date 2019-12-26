package tc.oc.named;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.identity.Identity;
import tc.oc.pgm.api.PGM;

public class NicknameRenderer implements NameRenderer {

  public static final ChatColor OFFLINE_COLOR = ChatColor.DARK_AQUA;
  public static final ChatColor ONLINE_COLOR = ChatColor.AQUA;
  public static final ChatColor DEAD_COLOR = ChatColor.DARK_GRAY;

  public String getTextName(Identity identity, NameType type) {
    if (identity.getNickname() != null && !type.reveal) {
      return identity.getNickname();
    } else {
      return identity.getRealName();
    }
  }

  @Override
  public ChatColor getColor(Identity identity, NameType type) {
    return type.dead && type.style.showDeath
        ? DEAD_COLOR
        : type.online ? ONLINE_COLOR : OFFLINE_COLOR;
  }

  @Override
  public String getLegacyName(Identity identity, NameType type) {
    String rendered = getTextName(identity, type);

    if (type.style.isColor) {
      rendered = getColor(identity, type) + rendered;
    }

    if (type.style.showDisguise && identity.getNickname() != null && type.reveal) {
      rendered = ChatColor.STRIKETHROUGH + rendered;
    }

    if (type.style.showFriend && type.friend && type.reveal) {
      rendered = ChatColor.ITALIC + rendered;
    }

    if (type.style.showPrefix && type.online && type.reveal) {
      final String prefix = PGM.get().getPrefixRegistry().getPrefix(identity.getPlayerId());
      if (prefix != null) {
        rendered = prefix + rendered;
      }
    }

    return rendered;
  }

  @Override
  public Component getComponentName(Identity identity, NameType type) {
    Component rendered = new PersonalizedText(getTextName(identity, type));

    if (type.style.showSelf && type.self && type.reveal) {
      rendered.setBold(true);
    }

    if (type.style.showFriend && type.friend && type.reveal) {
      rendered.setItalic(true);
    }

    if (type.style.showDisguise && identity.getNickname() != null && type.reveal) {
      rendered.setStrikethrough(true);

      if (type.style.showNickname) {
        rendered =
            new PersonalizedText(
                rendered, new PersonalizedText(" " + identity.getNickname(), ChatColor.ITALIC));
      }
    }

    if (type.style.isColor) {
      rendered.setColor(getColor(identity, type));
    }

    if (type.style.teleport) {
      BaseComponent dupe = rendered.duplicate();
      String name = identity.getNickname();
      if (name == null) name = identity.getRealName();
      rendered.clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + name));
      rendered.hoverEvent(
          HoverEvent.Action.SHOW_TEXT,
          new PersonalizedTranslatable("tip.teleportTo", dupe).render());
    }

    if (type.style.showPrefix && type.online && type.reveal) {
      final String prefix = PGM.get().getPrefixRegistry().getPrefix(identity.getPlayerId());
      if (prefix != null) {
        rendered = new PersonalizedText(new PersonalizedText(prefix), rendered);
      }
    }

    return rendered;
  }

  @Override
  public void invalidateCache(@Nullable Identity identity) {}
}

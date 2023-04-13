package tc.oc.pgm.util.player;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.named.NameDecorationProvider;
import tc.oc.pgm.util.named.NameStyle;

@SuppressWarnings("UnstableApiUsage")
public class PlayerRenderer {
  private static final TextColor DEAD_COLOR = NamedTextColor.DARK_GRAY;
  private static final Style NICK_STYLE =
      Style.style(TextDecoration.ITALIC).decoration(TextDecoration.STRIKETHROUGH, false);

  private final LoadingCache<PlayerCacheKey, Component> nameCache;

  protected PlayerRenderer() {
    this.nameCache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(
                new CacheLoader<PlayerCacheKey, Component>() {
                  @Override
                  public Component load(@NotNull PlayerCacheKey key) {
                    return render(key);
                  }
                });
  }

  public Component render(PlayerData data, PlayerRelationship relation) {
    return nameCache.getUnchecked(new PlayerCacheKey(data, relation));
  }

  public void decorationChanged(UUID uuid) {
    nameCache
        .asMap()
        .entrySet()
        .removeIf(
            entry -> {
              PlayerCacheKey key = entry.getKey();
              return key.relationship.reveal
                  && key.data.style.has(NameStyle.Flag.FLAIR)
                  && uuid.equals(key.data.uuid);
            });
  }

  private Component render(PlayerCacheKey key) {
    PlayerData data = key.data;
    PlayerRelationship relation = key.relationship;
    if (data.name == null) return PlayerComponent.UNKNOWN;

    // Generic term for either nicked or vanished
    boolean disguised = (data.nick != null || data.vanish);

    if (!data.online || (data.conceal && disguised && !relation.reveal)) {
      return text(data.name, PlayerComponent.OFFLINE_COLOR);
    }

    String plName = relation.reveal || data.nick == null ? data.name : data.nick;
    UUID uuid = data.uuid;

    TextColor color =
        data.style.has(NameStyle.Flag.DEATH) && data.dead
            ? DEAD_COLOR
            : data.style.has(NameStyle.Flag.COLOR) ? data.teamColor : null;

    TextComponent.Builder name = text().content(plName).color(color);

    if (relation.reveal && data.style.has(NameStyle.Flag.SELF) && relation.self) {
      name.decoration(TextDecoration.BOLD, true);
    }
    if (relation.reveal && data.style.has(NameStyle.Flag.FRIEND) && relation.friend) {
      name.decoration(TextDecoration.ITALIC, true);
    }
    if (data.style.has(NameStyle.Flag.SQUAD) && relation.squad) {
      name.decoration(TextDecoration.UNDERLINED, true);
    }
    if (relation.reveal && data.style.has(NameStyle.Flag.DISGUISE) && disguised) {
      name.decoration(TextDecoration.STRIKETHROUGH, true);

      if (data.nick != null && data.style.has(NameStyle.Flag.NICKNAME)) {
        name.append(text(" " + data.nick, NICK_STYLE));
      }
    }

    if (data.style.has(NameStyle.Flag.TELEPORT)) {
      name.hoverEvent(showText(translatable("misc.teleportTo", NamedTextColor.GRAY, name.build())))
          .clickEvent(runCommand("/tp " + plName));
    }

    if (relation.reveal && data.style.has(NameStyle.Flag.FLAIR)) {
      NameDecorationProvider provider = PGM.get().getNameDecorationRegistry();
      return textOfChildren(
          provider.getPrefixComponent(uuid), name, provider.getSuffixComponent(uuid));
    } else {
      // Optimization: if flairs aren't rendered, we can eliminate one nesting step
      return name.build();
    }
  }

  private static class PlayerCacheKey {
    public final PlayerData data;
    public final PlayerRelationship relationship;

    public PlayerCacheKey(PlayerData data, PlayerRelationship relationship) {
      this.data = data;
      this.relationship = relationship;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PlayerCacheKey)) return false;

      PlayerCacheKey that = (PlayerCacheKey) o;

      if (!Objects.equals(data, that.data)) return false;
      return Objects.equals(relationship, that.relationship);
    }

    @Override
    public int hashCode() {
      int result = data != null ? data.hashCode() : 0;
      result = 31 * result + (relationship != null ? relationship.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "PlayerCacheKey{" + "data=" + data + ", relationship=" + relationship + '}';
    }
  }
}

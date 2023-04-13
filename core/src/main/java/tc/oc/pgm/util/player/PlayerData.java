package tc.oc.pgm.util.player;

import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.named.NameStyle;

class PlayerData {
  public final @Nullable UUID uuid;

  public final @Nullable String name;
  public final @Nullable String nick;
  public final @NotNull TextColor teamColor;
  public final boolean dead;
  public final boolean vanish;
  public final boolean online;
  public final boolean conceal; // If player is disguise, pretend they are offline?
  public final @NotNull NameStyle style;

  public PlayerData(@NotNull Player player, @NotNull NameStyle style) {
    this.uuid = player.getUniqueId();
    this.name = player.getName();
    this.nick = Integration.getNick(player);
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
    this.teamColor = mp == null ? PlayerComponent.OFFLINE_COLOR : mp.getParty().getTextColor();
    this.dead = mp != null && mp.isDead();
    this.vanish = Integration.isVanished(player);
    this.online = player.isOnline();
    this.conceal = false;
    this.style = style;
  }

  public PlayerData(@NotNull MatchPlayer mp, @NotNull NameStyle style) {
    this.uuid = mp.getId();

    this.name = mp.getNameLegacy();
    this.nick = Integration.getNick(mp.getBukkit());
    this.teamColor =
        mp.getParty() == null ? PlayerComponent.OFFLINE_COLOR : mp.getParty().getTextColor();
    this.dead = mp.isDead();
    this.vanish = Integration.isVanished(mp.getBukkit());
    this.online = mp.getBukkit().isOnline();
    this.conceal = false;
    this.style = style;
  }

  public PlayerData(@NotNull MatchPlayerState mps, @NotNull NameStyle style) {
    this.uuid = mps.getId();

    this.name = mps.getNameLegacy();
    this.nick = mps.getNick();
    this.teamColor = mps.getParty().getTextColor();
    this.dead = mps.isDead();
    this.vanish = mps.isVanished();
    this.online = true; // They were online for sure when the state was created.
    this.conceal = false;
    this.style = style;
  }

  public PlayerData(@Nullable Player player, @Nullable String username, @NotNull NameStyle style) {
    this.uuid = player != null ? player.getUniqueId() : null;

    this.name = player != null ? player.getName() : username;
    this.nick = player != null ? Integration.getNick(player) : null;
    // Null-check is relevant as MatchManager will be null when loading author names.
    MatchPlayer mp = player != null ? PGM.get().getMatchManager().getPlayer(player) : null;
    this.teamColor = mp == null ? PlayerComponent.OFFLINE_COLOR : mp.getParty().getTextColor();
    this.dead = mp != null && mp.isDead();
    this.vanish = mp != null && Integration.isVanished(mp.getBukkit());
    this.online = player != null && player.isOnline();
    this.conceal = true;
    this.style = style;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerData)) return false;

    PlayerData that = (PlayerData) o;

    if (dead != that.dead) return false;
    if (vanish != that.vanish) return false;
    if (online != that.online) return false;
    if (conceal != that.conceal) return false;
    if (!Objects.equals(uuid, that.uuid)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(nick, that.nick)) return false;
    if (!teamColor.equals(that.teamColor)) return false;
    return style == that.style;
  }

  @Override
  public int hashCode() {
    int result = uuid != null ? uuid.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (nick != null ? nick.hashCode() : 0);
    result = 31 * result + teamColor.hashCode();
    result = 31 * result + (dead ? 1 : 0);
    result = 31 * result + (vanish ? 1 : 0);
    result = 31 * result + (online ? 1 : 0);
    result = 31 * result + (conceal ? 1 : 0);
    result = 31 * result + style.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "PlayerData{"
        + "uuid="
        + uuid
        + ", name='"
        + name
        + '\''
        + ", nick='"
        + nick
        + '\''
        + ", teamColor="
        + teamColor
        + ", dead="
        + dead
        + ", vanish="
        + vanish
        + ", online="
        + online
        + ", conceal="
        + conceal
        + ", style="
        + style
        + '}';
  }
}

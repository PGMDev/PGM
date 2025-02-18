package tc.oc.pgm.util.player;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.Component.virtual;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.VirtualComponentRenderer;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.named.NameStyle;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public final class PlayerComponent implements VirtualComponentRenderer<CommandSender> {

  public static final Component UNKNOWN =
      translatable("misc.unknown", PlayerRenderer.OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final Component CONSOLE =
      translatable("misc.console", PlayerRenderer.OFFLINE_COLOR, TextDecoration.ITALIC);

  public static final PlayerRenderer RENDERER = new PlayerRenderer();

  // The data for player being rendered.
  private final @Nullable Player player;
  private final @NotNull PlayerData data;

  private PlayerComponent(@Nullable Player player, @NotNull PlayerData data) {
    this.player = player;
    this.data = data;
  }

  public static Component player(@Nullable UUID playerId, @NotNull NameStyle style) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(playerId);
    if (player != null) return player(player, style);

    // Fallback to resolving the user
    return PGM.get().getDatastore().getUsername(playerId).getName(style);
  }

  public static Component player(CommandSender sender) {
    return player(sender, NameStyle.FANCY);
  }

  public static Component player(CommandSender sender, @NotNull NameStyle style) {
    if (sender == null) return UNKNOWN;
    if (sender instanceof Player p) return player(p, style);
    return CONSOLE;
  }

  public static Component player(Player player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN;
    return build(player, new PlayerData(player, style));
  }

  public static Component player(
      @Nullable Player player, @Nullable String username, @NotNull NameStyle style) {
    if (player == null && username == null) return UNKNOWN;
    return build(player, new PlayerData(player, username, style));
  }

  public static Component player(@Nullable MatchPlayerState player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN;
    return build(Bukkit.getPlayer(player.getId()), new PlayerData(player, style));
  }

  public static Component player(@Nullable MatchPlayer player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN;
    return build(player.getBukkit(), new PlayerData(player, style));
  }

  private static Component build(@Nullable Player player, @NotNull PlayerData data) {
    return virtual(CommandSender.class, new PlayerComponent(player, data));
  }

  @Override
  public @UnknownNullability ComponentLike apply(@NotNull CommandSender viewer) {
    // Render using a specialized render, which caches results
    return RENDERER.render(data, new PlayerRelationship(player, viewer));
  }

  @Override
  public @NotNull String fallbackString() {
    return "Unknown";
  }
}

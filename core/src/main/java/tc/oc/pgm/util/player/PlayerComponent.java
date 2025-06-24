package tc.oc.pgm.util.player;

import static net.kyori.adventure.text.Component.translatable;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.RenderableComponent;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public final class PlayerComponent implements RenderableComponent {

  public static final Component UNKNOWN =
      translatable("misc.unknown", PlayerRenderer.OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final Component CONSOLE =
      translatable("misc.console", PlayerRenderer.OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final PlayerComponent UNKNOWN_PLAYER =
      new PlayerComponent(null, new PlayerData(null, null, NameStyle.SIMPLE_COLOR), Style.empty());

  public static final PlayerRenderer RENDERER = new PlayerRenderer();

  // The data for player being rendered.
  private final @Nullable Player player;
  private final @NotNull PlayerData data;
  private final Style style;

  private PlayerComponent(@Nullable Player player, @NotNull PlayerData data, Style style) {
    this.player = player;
    this.data = data;
    this.style = style;
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
    if (sender == null) return UNKNOWN_PLAYER;
    if (sender instanceof Player p) return player(p, style);
    return CONSOLE;
  }

  public static PlayerComponent player(Player player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(player, new PlayerData(player, style), Style.empty());
  }

  public static Component player(
      @Nullable Player player, @Nullable String username, @NotNull NameStyle style) {
    if (player == null && username == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(player, new PlayerData(player, username, style), Style.empty());
  }

  public static PlayerComponent player(
      @Nullable MatchPlayerState player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(
        Bukkit.getPlayer(player.getId()), new PlayerData(player, style), Style.empty());
  }

  public static PlayerComponent player(@Nullable MatchPlayer player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(player.getBukkit(), new PlayerData(player, style), Style.empty());
  }

  public Component render(CommandSender viewer) {
    // Render using a specialized render, which caches results
    Component rendered = RENDERER.render(data, new PlayerRelationship(player, viewer));

    // If any specific styles were requested, add them in
    if (!style.isEmpty()) return rendered.style(rendered.style().merge(style));
    return rendered;
  }

  @Override
  public @NotNull RenderableComponent style(@NotNull Style style) {
    return new PlayerComponent(player, data, style);
  }

  @Override
  public @NotNull Style style() {
    return this.style;
  }

  @Override
  public @NotNull RenderableComponent colorIfAbsent(@Nullable TextColor color) {
    if (data.style.has(NameStyle.Flag.COLOR) || style.color() != null) return this;
    return new PlayerComponent(player, data, style.colorIfAbsent(color));
  }
}

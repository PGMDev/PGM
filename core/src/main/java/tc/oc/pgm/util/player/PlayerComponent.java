package tc.oc.pgm.util.player;

import static net.kyori.adventure.text.Component.translatable;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

  public static final TextColor OFFLINE_COLOR = NamedTextColor.DARK_AQUA;

  public static final Component UNKNOWN =
      translatable("misc.unknown", OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final Component CONSOLE = translatable("misc.console", OFFLINE_COLOR);
  public static final PlayerComponent UNKNOWN_PLAYER =
      new PlayerComponent(null, new PlayerData(null, null, NameStyle.PLAIN));

  public static final PlayerRenderer RENDERER = new PlayerRenderer();

  // The data for player being rendered.
  private final @Nullable Player player;
  private final @NotNull PlayerData data;

  private Style style = Style.empty();

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

  public static Component player(CommandSender sender, @NotNull NameStyle style) {
    if (sender == null) return UNKNOWN_PLAYER;
    if (sender instanceof Player) return player((Player) sender, style);
    return CONSOLE;
  }

  public static PlayerComponent player(Player player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(player, new PlayerData(player, style));
  }

  public static Component player(
      @Nullable Player player, @Nullable String username, @NotNull NameStyle style) {
    if (player == null && username == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(player, new PlayerData(player, username, style));
  }

  public static PlayerComponent player(
      @Nullable MatchPlayerState player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(Bukkit.getPlayer(player.getId()), new PlayerData(player, style));
  }

  public static PlayerComponent player(@Nullable MatchPlayer player, @NotNull NameStyle style) {
    if (player == null) return UNKNOWN_PLAYER;
    return new PlayerComponent(player.getBukkit(), new PlayerData(player, style));
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
    this.style = style;
    return this;
  }

  @Override
  public @NotNull Style style() {
    return this.style;
  }

  @Override
  public @NotNull RenderableComponent colorIfAbsent(@Nullable TextColor color) {
    if (!data.style.has(NameStyle.Flag.COLOR)) style = style.colorIfAbsent(color);
    return this;
  }
}

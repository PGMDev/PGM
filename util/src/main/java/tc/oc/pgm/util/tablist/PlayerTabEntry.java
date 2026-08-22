package tc.oc.pgm.util.tablist;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.event.player.PlayerSkinPartsChangeEvent;
import tc.oc.pgm.util.skin.Skin;

/**
 * {@link TabEntry} showing a {@link Player}'s name and skin.
 *
 * <p>Note that this is NOT the player's real entry. It has a random UUID and name, like any other
 * {@link SimpleTabEntry}. While this entry is visible in a {@link TabView}, a fake player entity
 * will be spawned with a copy of the real player's metadata.
 */
public class PlayerTabEntry extends DynamicTabEntry {

  private static boolean showPing = false;
  private static Function<Player, Component> playerComponent = p -> text(p.getName());
  private static BiFunction<Player, Player, @Nullable Skin> playerSkin = (player, viewer) -> null;

  public static void setPlayerComponent(Function<Player, Component> playerComponent) {
    PlayerTabEntry.playerComponent = playerComponent;
  }

  /** Set which skin each viewer is shown, returning null for the player's own */
  public static void setPlayerSkin(BiFunction<Player, Player, @Nullable Skin> playerSkin) {
    PlayerTabEntry.playerSkin = playerSkin;
  }

  public static void setShowRealPing(boolean showPing) {
    PlayerTabEntry.showPing = showPing;
  }

  private static UUID randomUUIDVersion2SameDefaultSkin(UUID original) {
    // Parity of UUID.hashCode determines if the player's default skin is Steve/Alex
    // To make the player list match, we generate a random UUID with the same hashCode parity.
    // UUID.hashCode returns the XOR of its four 32-bit segments, so set bit 0 to the desired
    // parity, and clear bits 32, 64, and 96

    long parity = original.hashCode() & 1L;
    long mask = ~((1L << 32) | 1L);
    UUID uuid = randomUUIDVersion2();
    uuid = new UUID(
        uuid.getMostSignificantBits() & mask, (uuid.getLeastSignificantBits() & mask) | parity);
    return uuid;
  }

  /** An entity ID and the world it was allocated from */
  private record ViewEntity(int entityId, UUID worldId) {}

  protected final Player player;

  // As entity IDs are allocated per-world on 26.2+, they are also only guaranteed unique
  // in the world they were allocated in. We have to re-allocate on world change.
  private final Map<TabView, ViewEntity> viewEntities = new WeakHashMap<>();

  public PlayerTabEntry(Player player) {
    super(randomUUIDVersion2SameDefaultSkin(player.getUniqueId()));
    this.player = player;
  }

  @Override
  public Component getContent(TabView view) {
    return playerComponent.apply(player);
  }

  @Override
  public int getFakeEntityId(TabView view) {
    ViewEntity entity = this.viewEntities.get(view);
    return entity == null ? NULL_ENTITY : entity.entityId();
  }

  @Override
  public int allocateFakeEntityId(TabView view) {
    World world = view.getViewer().getWorld();
    ViewEntity entity = this.viewEntities.get(view);

    if (entity == null || !entity.worldId().equals(world.getUID())) {
      entity = new ViewEntity(NMS_HACKS.allocateEntityId(world), world.getUID());
      this.viewEntities.put(view, entity);
    }
    return entity.entityId();
  }

  @Override
  public Player getFakePlayer(TabView view) {
    return this.player;
  }

  @Override
  public Skin getSkin(TabView view) {
    Player viewer = view.getViewer();
    if (viewer == null) {
      return null;
    }
    Skin skin = playerSkin.apply(player, viewer);
    return skin != null ? skin : PLAYER_UTILS.getPlayerSkin(player);
  }

  @Override
  public int getPing() {
    if (showPing) return PLAYER_UTILS.getPing(this.player);
    return super.getPing();
  }

  // Dispatched by TabManager
  protected void onSkinPartsChange(PlayerSkinPartsChangeEvent event) {
    if (this.player == event.getPlayer()) {
      this.updateFakeEntity();
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" + this.player.getName() + "}";
  }
}

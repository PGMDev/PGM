package tc.oc.pgm.util.tablist;

import static net.kyori.adventure.text.Component.text;

import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.event.player.PlayerSkinPartsChangeEvent;
import tc.oc.pgm.util.nms.NMSHacks;
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

  public static void setPlayerComponent(Function<Player, Component> playerComponent) {
    PlayerTabEntry.playerComponent = playerComponent;
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
    uuid =
        new UUID(
            uuid.getMostSignificantBits() & mask, (uuid.getLeastSignificantBits() & mask) | parity);
    return uuid;
  }

  protected final Player player;
  private final int spareEntityId;

  public PlayerTabEntry(Player player) {
    super(randomUUIDVersion2SameDefaultSkin(player.getUniqueId()));
    this.player = player;
    this.spareEntityId = NMSHacks.allocateEntityId();
  }

  @Override
  public Component getContent(TabView view) {
    return playerComponent.apply(player);
  }

  @Override
  public int getFakeEntityId(TabView view) {
    return this.spareEntityId;
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

    // TODO: find different solution for non-SportPaper servers
    return this.player.hasFakeSkin(viewer)
        ? new Skin(
            this.player.getFakeSkin(viewer).getData(),
            this.player.getFakeSkin(viewer).getSignature())
        : NMSHacks.getPlayerSkin(this.player);
  }

  @Override
  public int getPing() {
    if (showPing) return NMSHacks.getPing(this.player);
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

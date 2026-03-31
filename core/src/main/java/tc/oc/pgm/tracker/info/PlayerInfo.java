package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.named.NameStyle;

public record PlayerInfo(ParticipantState player, @Nullable ItemInfo weapon)
    implements OwnerInfo, MeleeInfo, PhysicalInfo {

  public PlayerInfo {
    assertNotNull(player);
  }

  public PlayerInfo(ParticipantState player) {
    this(player, null);
  }

  public PlayerInfo(MatchPlayer player) {
    this(player.getParticipantState(), new ItemInfo(player.getInventory().getItemInHand()));
  }

  @Override
  public ParticipantState owner() {
    return player();
  }

  @Override
  public ParticipantState attacker() {
    return player();
  }

  @Override
  public String identifier() {
    return player.getId().toString();
  }

  @Override
  public Component name() {
    return player.getName(NameStyle.COLOR);
  }

  @Override
  public @NonNull String toString() {
    return getClass().getSimpleName() + "{player=" + attacker() + " weapon=" + weapon() + "}";
  }
}

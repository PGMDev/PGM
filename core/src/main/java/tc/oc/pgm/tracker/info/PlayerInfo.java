package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.named.NameStyle;

public class PlayerInfo implements OwnerInfo, MeleeInfo, PhysicalInfo {

  private final ParticipantState player;
  private final ItemInfo weapon;

  public PlayerInfo(ParticipantState player, @Nullable ItemInfo weapon) {
    this.player = assertNotNull(player);
    this.weapon = weapon;
  }

  public PlayerInfo(ParticipantState player) {
    this(player, null);
  }

  public PlayerInfo(MatchPlayer player) {
    this(player.getParticipantState(), new ItemInfo(player.getInventory().getItemInHand()));
  }

  @Override
  public @Nullable ItemInfo getWeapon() {
    return weapon;
  }

  @Override
  public ParticipantState getOwner() {
    return player;
  }

  @Override
  public ParticipantState getAttacker() {
    return player;
  }

  @Override
  public String getIdentifier() {
    return player.getId().toString();
  }

  @Override
  public Component getName() {
    return player.getName(NameStyle.COLOR);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{player=" + getAttacker() + " weapon=" + getWeapon() + "}";
  }
}

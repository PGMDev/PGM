package tc.oc.pgm.tracker.damage;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import tc.oc.component.Component;
import tc.oc.named.NameStyle;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.ParticipantState;

public class PlayerInfo implements OwnerInfo, MeleeInfo, PhysicalInfo {

  private final ParticipantState player;
  private final ItemInfo weapon;

  public PlayerInfo(ParticipantState player, @Nullable ItemInfo weapon) {
    this.player = checkNotNull(player);
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
    return player.getPlayerId().toString();
  }

  @Override
  public Component getLocalizedName() {
    return player.getStyledName(NameStyle.COLOR);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{player=" + getAttacker() + " weapon=" + getWeapon() + "}";
  }
}

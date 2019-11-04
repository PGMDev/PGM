package tc.oc.pgm.api.player.event;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.tracker.damage.DamageInfo;

/** Called when {@link MatchPlayer} dies, the victim is the {@link #getPlayer()}. */
public class MatchPlayerDeathEvent extends MatchPlayerEvent {

  private final PlayerDeathEvent parent;
  private final DamageInfo damageInfo;
  private final boolean predicted;

  public MatchPlayerDeathEvent(
      PlayerDeathEvent parent, MatchPlayer victim, DamageInfo damageInfo, boolean predicted) {
    super(checkNotNull(victim));
    this.parent = checkNotNull(parent);
    this.damageInfo = checkNotNull(damageInfo);
    this.predicted = predicted;
  }

  /**
   * Get the {@link org.bukkit.Bukkit} {@link PlayerDeathEvent}.
   *
   * @return The parent event.
   */
  public final PlayerDeathEvent getParent() {
    return parent;
  }

  /**
   * Get victim {@link MatchPlayer} of the {@link MatchPlayerDeathEvent}.
   *
   * @return The victim.
   */
  public final MatchPlayer getVictim() {
    return getPlayer();
  }

  /**
   * Get the optional {@link ParticipantState} of the killer.
   *
   * @return The killer {@link ParticipantState}, or {@code null} if no killer.
   */
  public final @Nullable ParticipantState getKiller() {
    return damageInfo.getAttacker();
  }

  /**
   * Get the {@link DamageInfo} of how the death occurred.
   *
   * @return The {@link DamageInfo}.
   */
  public final DamageInfo getDamageInfo() {
    return damageInfo;
  }

  /**
   * Get whether the {@link MatchPlayer} death was predicted.
   *
   * @return Whether the {@link MatchPlayer} victim is offline.
   */
  public final boolean isPredicted() {
    return predicted;
  }

  /**
   * Get whether the given {@link MatchPlayer} is the victim.
   *
   * @param player The {@link MatchPlayer} to check.
   * @return Whether the {@link MatchPlayer} is the victim.
   */
  public final boolean isVictim(MatchPlayer player) {
    return getVictim().equals(player);
  }

  /**
   * Get whether the given {@link MatchPlayer} is the killer.
   *
   * @param player The {@link MatchPlayer} to check.
   * @return Whether the {@link MatchPlayer} is the killer.
   */
  public final boolean isKiller(MatchPlayer player) {
    ParticipantState killer = getKiller();
    return killer != null && killer.isPlayer(player);
  }

  /**
   * Get whether the {@link MatchPlayer} is either a victim or killer.
   *
   * @param player The {@link MatchPlayer} to check.
   * @return Whether the {@link MatchPlayer} is involved.
   */
  public final boolean isInvolved(MatchPlayer player) {
    return isVictim(player) || isKiller(player);
  }

  /** Get the relationship between the victim and killer */
  public final PlayerRelation getRelation() {
    return PlayerRelation.get(getVictim().getParticipantState(), getKiller());
  }

  /**
   * Get whether the death was caused by a teammate.
   *
   * @return Whether the death was from a teammate.
   */
  public final boolean isTeamKill() {
    return PlayerRelation.ALLY == getRelation();
  }

  /**
   * Get whether the dead was caused by an enemy.
   *
   * @return Whether the dead was from an enemy.
   */
  public final boolean isEnemyKill() {
    return PlayerRelation.ENEMY == getRelation();
  }

  /**
   * Get whether the {@link MatchPlayer} killed themselves.
   *
   * @return Whether the death was suicide.
   */
  public final boolean isSuicide() {
    return PlayerRelation.SELF == getRelation();
  }

  /**
   * Get whether the victim and killer are the same {@link MatchPlayer}.
   *
   * @return
   */
  public final boolean isSelfKill() {
    return getKiller() != null && getKiller().isPlayer(getVictim());
  }

  /**
   * Get whether the death was from an enemy and it was no caused by suicide.
   *
   * @return Whether the death was actually "challenging."
   */
  public final boolean isChallengeKill() {
    return isEnemyKill() && !isSelfKill();
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}

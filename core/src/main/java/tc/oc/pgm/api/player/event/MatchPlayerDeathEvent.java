package tc.oc.pgm.api.player.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.tracker.info.DamageInfo;

/** Called when {@link MatchPlayer} dies, the victim is the {@link #getPlayer()}. */
public class MatchPlayerDeathEvent extends MatchPlayerEvent {

  private final PlayerDeathEvent parent;
  private final DamageInfo damageInfo;
  private final boolean predicted;
  private @Nullable final ParticipantState assister;

  public MatchPlayerDeathEvent(
      PlayerDeathEvent parent,
      MatchPlayer victim,
      DamageInfo damageInfo,
      boolean predicted,
      @Nullable ParticipantState assister) {
    super(assertNotNull(victim));
    this.parent = assertNotNull(parent);
    this.damageInfo = assertNotNull(damageInfo);
    this.predicted = predicted;
    this.assister = assister;
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
   * Get the {@link ParticipantState} of the assisting killer.
   *
   * @return The assister {@link ParticipantState}, or {@code null} if no killer.
   */
  public @Nullable ParticipantState getAssister() {
    return assister;
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
    if (player == null) return false;
    return isVictim(player) || isKiller(player);
  }

  /** Get the relationship between the victim and killer */
  public final PlayerRelation getRelation() {
    return PlayerRelation.get(getVictim().getParticipantState(), getKiller());
  }

  /** Get the relationship between the victim and assister */
  public final PlayerRelation getAssistRelation() {
    return PlayerRelation.get(getVictim().getParticipantState(), getAssister());
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
   * Get whether the assist was caused by an enemy.
   *
   * @return Whether the assist was from an enemy.
   */
  public final boolean isEnemyAssist() {
    return PlayerRelation.ENEMY == getAssistRelation();
  }

  /**
   * Get whether the {@link MatchPlayer} killed themselves.
   *
   * @return Whether the death was suicide.
   */
  public final boolean isSuicide() {
    return PlayerRelation.SELF == getRelation();
  }

  /** Get whether the victim and killer are the same {@link MatchPlayer}. */
  public final boolean isSelfKill() {
    return getKiller() != null && getKiller().isPlayer(getVictim());
  }

  public final boolean isSelfAssist() {
    return getAssister() != null && getAssister().isPlayer(getVictim());
  }

  /**
   * Get whether the death was from an enemy and it was not caused by suicide.
   *
   * @return Whether the death was actually "challenging."
   */
  public final boolean isChallengeKill() {
    return isEnemyKill() && !isSelfKill();
  }

  /**
   * Get whether the assist was from an enemy and it was not caused by suicide.
   *
   * @return Whether the assist was actually "challenging."
   */
  public final boolean isChallengeAssist() {
    return isEnemyAssist() && !isSelfAssist();
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

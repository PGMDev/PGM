package tc.oc.pgm.damagehistory;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class DamageEntry {

  @Nullable
  private ParticipantState damager;

  private double damage;

  public DamageEntry(@Nullable ParticipantState damager, double damage) {
    this.damager = damager;
    this.damage = damage;
  }

  @Nullable
  public ParticipantState getDamager() {
    return damager;
  }

  public double getDamage() {
    return damage;
  }

  public void addDamage(@Nullable ParticipantState damager, double damage) {
    this.damager = damager;
    this.damage += damage;
  }

  public void removeDamage(double damage) {
    this.damage -= damage;
  }

  public boolean canAssist(MatchPlayer victim, ParticipantState killer) {
    // Filter out damage without players, or damage from self or killer
    if (damager == null) return false;
    UUID dmg = damager.getId();
    return damager != null && !dmg.equals(victim.getId()) && !dmg.equals(killer.getId());
  }
}

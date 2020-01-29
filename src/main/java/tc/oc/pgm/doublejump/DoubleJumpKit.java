package tc.oc.pgm.doublejump;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.joda.time.Duration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.AbstractKit;

public class DoubleJumpKit extends AbstractKit {
  public static final float DEFAULT_POWER = 3f; // mainly for backward compatibility
  public static final Duration DEFAULT_RECHARGE = Duration.millis(2500);

  protected final boolean enabled;
  protected final float power; // 1 power is roughly a normal vanilla jump
  protected final Duration rechargeTime;
  protected final boolean rechargeInAir;

  public DoubleJumpKit(boolean enabled, float power, Duration rechargeTime, boolean rechargeInAir) {
    this.enabled = enabled;
    this.power = power;
    this.rechargeTime = rechargeTime;
    this.rechargeInAir = rechargeInAir;
  }

  @Override
  public void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    DoubleJumpMatchModule djmm = player.getMatch().getModule(DoubleJumpMatchModule.class);
    if (djmm != null) djmm.setKit(player.getBukkit(), this);
  }

  public float chargePerTick() {
    return 50F / this.rechargeTime.getMillis();
  }

  public boolean needsRecharge() {
    return !Duration.ZERO.equals(this.rechargeTime);
  }
}

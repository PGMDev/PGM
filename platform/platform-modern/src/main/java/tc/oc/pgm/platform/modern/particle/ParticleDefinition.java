package tc.oc.pgm.platform.modern.particle;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.platform.modern.particle.shapes.ParticleShape;

public class ParticleDefinition extends SelfIdentifyingFeatureDefinition {
  public static final int INFINITE_PARTICLE_AMOUNT = 99;
  private final Particle type;
  private final int amount;
  private final Particle.DustOptions dustOptions;

  @Nullable
  private final Color color;

  private final boolean teamColor;

  private final Particle.Spell power;

  @Nullable
  private final Float angle;

  @Nullable
  private final Integer delay;

  @Nullable
  private final Float breathPower;

  @Nullable
  private final BlockData blockData;

  @Nullable
  private final ItemStack itemStack;

  @Nullable
  private final Filter viewerFilter;

  private final boolean force;

  @Nullable
  private final ParticleShape shape;

  public ParticleDefinition(
      @Nullable String id,
      @Nullable Particle type,
      int amount,
      Particle.DustOptions dustOptions,
      @Nullable Color color,
      boolean teamColor,
      Particle.Spell power,
      @Nullable Float angle,
      @Nullable Integer delay,
      @Nullable Float breathPower,
      @Nullable BlockData blockData,
      @Nullable ItemStack itemStack,
      @Nullable Filter viewerFilter,
      boolean force,
      @Nullable ParticleShape shape) {

    super(id);
    this.type = type;
    this.amount = amount;
    this.dustOptions = dustOptions;
    this.color = color;
    this.teamColor = teamColor;
    this.power = power;
    this.angle = angle;
    this.delay = delay;
    this.breathPower = breathPower;
    this.blockData = blockData;
    this.itemStack = itemStack;
    this.viewerFilter = viewerFilter;
    this.force = force;
    this.shape = shape;
  }

  public Particle getType() {
    return type;
  }

  public int getAmount() {
    return amount;
  }

  public Particle.DustOptions getDustOptions() {
    return dustOptions;
  }

  public @Nullable Color getColor() {
    return color;
  }

  public boolean isTeamColor() {
    return teamColor;
  }

  public Particle.Spell getPower() {
    return power;
  }

  public @Nullable Float getAngle() {
    return angle;
  }

  public @Nullable Integer getDelay() {
    return delay;
  }

  public @Nullable Float getBreathPower() {
    return breathPower;
  }

  public @Nullable BlockData blockData() {
    return blockData;
  }

  public @Nullable ItemStack itemStack() {
    return itemStack;
  }

  public @Nullable Filter getViewerFilter() {
    return viewerFilter;
  }

  public boolean isForce() {
    return force;
  }

  public @Nullable ParticleShape getShape() {
    return shape;
  }
}

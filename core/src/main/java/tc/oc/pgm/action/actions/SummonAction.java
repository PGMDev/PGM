package tc.oc.pgm.action.actions;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.entity.TaggedMob;
import tc.oc.pgm.entity.TaggedMobMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;

public class SummonAction<B extends Filterable<?>> extends AbstractAction<B> {

  private final @Nullable FeatureReference<TaggedMob> entity;
  private final @Nullable SpawnableEntity mob;
  private final Formula<B> xformula;
  private final Formula<B> yformula;
  private final Formula<B> zformula;
  private final Optional<Formula<B>> pitchFormula;
  private final Optional<Formula<B>> yawFormula;

  public SummonAction(
      Class<B> scope,
      @Nullable FeatureReference<TaggedMob> entity,
      @Nullable SpawnableEntity mob,
      Formula<B> xformula,
      Formula<B> yformula,
      Formula<B> zformula,
      Optional<Formula<B>> pitchFormula,
      Optional<Formula<B>> yawFormula) {
    super(scope);
    this.entity = entity;
    this.mob = mob;
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.pitchFormula = pitchFormula;
    this.yawFormula = yawFormula;
  }

  @Override
  public void trigger(B b) {
    Match match = b.getMatch();
    double x = xformula.apply(b);
    double y = yformula.apply(b);
    double z = zformula.apply(b);
    float pitch = pitchFormula.map(f -> (float) f.apply(b)).orElse(0f);
    float yaw = yawFormula.map(f -> (float) f.apply(b)).orElse(0f);

    if (entity != null) {
      entity.get().spawn(match, x, y, z, pitch, yaw);
      return;
    }

    if (mob != null) {
      Entity spawned = mob.spawn(new Location(match.getWorld(), x, y, z, pitch, yaw));

      if (mob.id() != null && spawned instanceof LivingEntity le) {
        match.needModule(TaggedMobMatchModule.class).track(mob.id(), le);
      }
      return;
    }
  }
}

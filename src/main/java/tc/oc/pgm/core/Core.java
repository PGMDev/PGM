package tc.oc.pgm.core;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.ModeChangeGoal;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.modes.ModeUtils;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.teams.Team;
import tc.oc.util.components.Components;

public class Core extends TouchableGoal<CoreFactory> implements ModeChangeGoal<CoreFactory> {

  protected final FiniteBlockRegion casingRegion;
  protected final FiniteBlockRegion lavaRegion;
  protected final Region leakRegion;

  protected MaterialData material;
  protected boolean leaked = false;
  protected Iterable<Location> proximityLocations;

  public Core(CoreFactory definition, Match match) {
    super(definition, match);

    this.material = definition.getMaterial();

    Region region = definition.getRegion();

    this.casingRegion =
        FiniteBlockRegion.fromWorld(
            region,
            match.getWorld(),
            match.getMap().getProto(),
            new SingleMaterialMatcher(this.material));
    if (this.casingRegion.getBlocks().isEmpty()) {
      match
          .getLogger()
          .warning("No casing world (" + this.material + ") found in core " + this.getName());
    }

    this.lavaRegion =
        FiniteBlockRegion.fromWorld(
            region,
            match.getWorld(),
            match.getMap().getProto(),
            new SingleMaterialMatcher(Material.LAVA, (byte) 0),
            new SingleMaterialMatcher(Material.STATIONARY_LAVA, (byte) 0));
    if (this.lavaRegion.getBlocks().isEmpty()) {
      match.getLogger().warning("No lava found in core " + this.getName());
    }

    Vector min = region.getBounds().getMin().subtract(new Vector(15, 0, 15));
    min.setY(0);
    Vector max = region.getBounds().getMax().add(new Vector(15, 0, 15));
    max.setY(region.getBounds().getMin().getY() - definition.getLeakLevel());
    this.leakRegion = new CuboidRegion(min, max);
  }

  // Remove @Nullable
  @Override
  public @Nonnull Team getOwner() {
    return super.getOwner();
  }

  @Override
  public boolean getDeferTouches() {
    return true;
  }

  @Override
  public Component getTouchMessage(@Nullable ParticipantState toucher, boolean self) {
    if (toucher == null) {
      return new PersonalizedTranslatable(
          "match.touch.core.owner",
          Components.blank(),
          getComponentName(),
          getOwner().getComponentName());
    } else if (self) {
      return new PersonalizedTranslatable(
          "match.touch.core.owner.you",
          Components.blank(),
          getComponentName(),
          getOwner().getComponentName());
    } else {
      return new PersonalizedTranslatable(
          "match.touch.core.owner.toucher",
          toucher.getStyledName(NameStyle.COLOR),
          getComponentName(),
          getOwner().getComponentName());
    }
  }

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    if (proximityLocations == null) {
      proximityLocations =
          Collections.singleton(
              casingRegion.getBounds().getCenterPoint().toLocation(this.getMatch().getWorld()));
    }
    return proximityLocations;
  }

  public MaterialData getMaterial() {
    return this.material;
  }

  public FiniteBlockRegion getCasingRegion() {
    return this.casingRegion;
  }

  public FiniteBlockRegion getLavaRegion() {
    return this.lavaRegion;
  }

  public Region getLeakRegion() {
    return this.leakRegion;
  }

  public void markLeaked() {
    this.leaked = true;
  }

  public boolean hasLeaked() {
    return this.leaked;
  }

  @Override
  public boolean isShared() {
    return false;
  }

  @Override
  public boolean canComplete(Competitor team) {
    return team != this.getOwner();
  }

  @Override
  public boolean isCompleted() {
    return this.leaked;
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return this.leaked && this.canComplete(team);
  }

  @Override
  public boolean isAffectedByModeChanges() {
    return this.definition.hasModeChanges();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void replaceBlocks(MaterialData newMaterial) {
    for (Block block : this.getCasingRegion().getBlocks()) {
      if (this.isObjectiveMaterial(block)) {
        block.setTypeIdAndData(newMaterial.getItemTypeId(), newMaterial.getData(), true);
      }
    }
    this.material = newMaterial;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isObjectiveMaterial(Block block) {
    return block.getType() == this.material.getItemType()
        && block.getData() == this.material.getData();
  }

  @Override
  public String getModeChangeMessage(Material material) {
    return ModeUtils.formatMaterial(material) + " CORE MODE";
  }

  public ImmutableList<Contribution> getContributions() {
    Set<ParticipantState> touchers = getTouchingPlayers();
    ImmutableList.Builder<Contribution> builder = ImmutableList.builder();
    for (MatchPlayerState player : touchers) {
      builder.add(new Contribution(player, 1d / touchers.size()));
    }
    return builder.build();
  }
}

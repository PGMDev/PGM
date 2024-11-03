package tc.oc.pgm.core;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.Style.style;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.IncrementalGoal;
import tc.oc.pgm.goals.ModeChangeGoal;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.modes.ModeUtils;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.named.NameStyle;

// TODO: Consider making Core extend Destroyable
public class Core extends TouchableGoal<CoreFactory>
    implements IncrementalGoal<CoreFactory>, ModeChangeGoal<CoreFactory> {

  private static final MaterialMatcher LAVA_BLOCKS =
      MaterialMatcher.of(Material.LAVA, Materials.STILL_LAVA);

  protected final FiniteBlockRegion casingRegion;
  protected final FiniteBlockRegion lavaRegion;
  protected final Region leakRegion;
  protected final int leakRequired;
  protected final boolean isShared;

  protected MaterialMatcher material;
  protected int leak = 0;
  protected boolean leaked = false;
  protected Iterable<Location> proximityLocations;

  public Core(CoreFactory definition, Match match) {
    super(definition, match);

    this.material = definition.getMaterial();

    Region region = definition.getRegion();

    this.casingRegion = FiniteBlockRegion.fromWorld(
        region, match.getWorld(), this.material, match.getMap().getProto());
    if (this.casingRegion.getBlockVolume() == 0) {
      match
          .getLogger()
          .warning("No casing world (" + this.material + ") found in core " + this.getName());
    }

    this.lavaRegion = FiniteBlockRegion.fromWorld(
        region, match.getWorld(), LAVA_BLOCKS, match.getMap().getProto());
    if (this.lavaRegion.getBlockVolume() == 0) {
      match.getLogger().warning("No lava found in core " + this.getName());
    }

    Vector min = region.getBounds().getMin().subtract(new Vector(15, 0, 15));
    min.setY(0);
    Vector max = region.getBounds().getMax().add(new Vector(15, 0, 15));
    max.setY(region.getBounds().getMin().getY() - definition.getLeakLevel());
    this.leakRegion = new CuboidRegion(min, max);

    this.leakRequired = lavaRegion.getBounds().getMin().getBlockY() - max.getBlockY() + 1;
    this.isShared = match.getCompetitors().stream().filter(this::canComplete).count() != 1;
  }

  // Remove @Nullable
  @Override
  public @NotNull Team getOwner() {
    Team owner = super.getOwner();
    if (owner == null) {
      throw new IllegalStateException("core " + getId() + " has no owner");
    }
    return owner;
  }

  @Override
  public boolean getDeferTouches() {
    return true;
  }

  @Override
  public Component getTouchMessage(@Nullable ParticipantState toucher, boolean self) {
    // Core has same touch messages as Destroyable
    if (toucher == null) {
      return translatable(
          "destroyable.touch.owned", empty(), getComponentName(), getOwner().getName());
    } else if (self) {
      return translatable(
          "destroyable.touch.owned.you", empty(), getComponentName(), getOwner().getName());
    } else {
      return translatable(
          "destroyable.touch.owned.player",
          toucher.getName(NameStyle.COLOR),
          getComponentName(),
          getOwner().getName());
    }
  }

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    if (proximityLocations == null) {
      proximityLocations = Collections.singleton(
          casingRegion.getBounds().getCenterPoint().toLocation(this.getMatch().getWorld()));
    }
    return proximityLocations;
  }

  @Override
  public boolean isAffectedBy(Mode mode) {
    return mode.getMaterialData() != null
        && (this.definition.getModes() == null || this.definition.getModes().contains(mode));
  }

  public boolean isCoreMaterial(MaterialData material) {
    return this.material.matches(material);
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

  public boolean updateLeak(int yLevel) {
    int newLeak = Math.min(lavaRegion.getBounds().getMin().getBlockY() - yLevel, leakRequired);
    if (newLeak > this.leak) {
      this.leak = newLeak;
      return true;
    }
    return false;
  }

  public void markLeaked() {
    this.leaked = true;
  }

  public boolean hasLeaked() {
    return this.leaked;
  }

  @Override
  public boolean isShared() {
    return isShared;
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
  public boolean getShowProgress() {
    return this.definition.getShowProgress();
  }

  @Override
  public double getCompletion() {
    return (double) this.leak / this.leakRequired;
  }

  @Override
  public String renderCompletion() {
    return StringUtils.percentage(this.getCompletion());
  }

  @Nullable
  @Override
  public String renderPreciseCompletion() {
    return this.leak + "/" + this.leakRequired;
  }

  @Override
  public Component renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (this.getShowProgress() || viewer.isObserving()) {
      if (!PGM.get().getConfiguration().showProximity()) {
        return text(this.renderCompletion());
      }
      return text()
          .content(this.renderCompletion())
          .append(space())
          .append(text(this.renderPreciseCompletion(), style(NamedTextColor.GRAY)))
          .build();
    } else {
      return super.renderSidebarStatusText(competitor, viewer);
    }
  }

  @Override
  public void replaceBlocks(BlockMaterialData newMaterial) {
    for (Block block : this.getCasingRegion().getBlocks(match.getWorld())) {
      if (this.isObjectiveMaterial(block)) {
        newMaterial.applyTo(block, true);
      }
    }
    this.material = newMaterial.toMatcher();
  }

  @Override
  public boolean isObjectiveMaterial(Block block) {
    return material.matches(block.getState());
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

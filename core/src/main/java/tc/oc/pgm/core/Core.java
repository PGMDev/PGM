package tc.oc.pgm.core;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
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
import tc.oc.pgm.modes.ModeUtils;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.named.NameStyle;

// TODO: Consider making Core extend Destroyable
public class Core extends TouchableGoal<CoreFactory>
    implements IncrementalGoal<CoreFactory>, ModeChangeGoal<CoreFactory> {

  protected final FiniteBlockRegion casingRegion;
  protected final FiniteBlockRegion lavaRegion;
  protected final Region leakRegion;
  protected final int leakRequired;

  protected MaterialData material;
  protected int leak = 0;
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

    this.leakRequired = lavaRegion.getBounds().getMin().getBlockY() - max.getBlockY() + 1;
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
    // Core has same touch messages as Destroyable
    if (toucher == null) {
      return TranslatableComponent.of(
          "destroyable.touch.owned",
          TextComponent.empty(),
          getComponentName(),
          getOwner().getName());
    } else if (self) {
      return TranslatableComponent.of(
          "destroyable.touch.owned.you",
          TextComponent.empty(),
          getComponentName(),
          getOwner().getName());
    } else {
      return TranslatableComponent.of(
          "destroyable.touch.owned.player",
          toucher.getName(NameStyle.COLOR),
          getComponentName(),
          getOwner().getName());
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
  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (this.getShowProgress() || viewer.isObserving()) {
      String text = this.renderCompletion();
      if (PGM.get().getConfiguration().showProximity()) {
        String precise = this.renderPreciseCompletion();
        if (precise != null) {
          text += " " + ChatColor.GRAY + precise;
        }
      }
      return text;
    } else {
      return super.renderSidebarStatusText(competitor, viewer);
    }
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

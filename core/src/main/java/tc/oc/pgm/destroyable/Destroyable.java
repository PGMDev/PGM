package tc.oc.pgm.destroyable;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.Style.style;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Firework;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.blockdrops.BlockDrops;
import tc.oc.pgm.blockdrops.BlockDropsMatchModule;
import tc.oc.pgm.blockdrops.BlockDropsRuleSet;
import tc.oc.pgm.events.FeatureChangeEvent;
import tc.oc.pgm.fireworks.FireworkMatchModule;
import tc.oc.pgm.goals.IncrementalGoal;
import tc.oc.pgm.goals.ModeChangeGoal;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.modes.ModeUtils;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.named.NameStyle;

public class Destroyable extends TouchableGoal<DestroyableFactory>
    implements IncrementalGoal<DestroyableFactory>, ModeChangeGoal<DestroyableFactory> {

  // Block replacement rules in this ruleset will be used to calculate destroyable health
  protected BlockDropsRuleSet blockDropsRuleSet;

  protected final FiniteBlockRegion blockRegion;
  protected MaterialMatcher materialPattern;
  protected Set<BlockMaterialData> materials;
  protected final boolean isShared;

  // The percentage of blocks that must be broken for the entire Destroyable to be destroyed.
  protected double destructionRequired;

  protected final Duration SPARK_COOLDOWN = Duration.ofMillis(75);
  protected Instant lastSparkTime;

  /**
   * The maximum possible health that this Destroyable can have, which is the sum of the max health
   * of each block. The max health of a block is the maximum number of breaks between any
   * destroyable world and any non-destroyable world. Note that blocks are not necessarily at max
   * health when the match starts. This value can change as the result of mode changes.
   */
  protected int maxHealth;

  // The current health of the Destroyable
  protected int health;

  /**
   * Map of block -> world -> health i.e. the health level that each world represents for each block
   * in the destroyable. For example, (1,2,3) -> Gold Block -> 3 means that when there is a gold
   * block at (1,2,3), it will need to be broken three times to change into a block that is not a
   * destroyable world.
   *
   * <p>This map will have en entry for every destroyable world for every block. If there are no
   * custom block replacement rules affecting this destroyable, this will be null;
   */
  protected Map<BlockVector, Map<BlockMaterialData, Integer>> blockMaterialHealth;

  protected final List<DestroyableHealthChange> events = Lists.newArrayList();
  protected ImmutableList<DestroyableContribution> contributions;

  protected Iterable<Location> proximityLocations;

  public Destroyable(DestroyableFactory definition, Match match) {
    super(definition, match);

    this.materialPattern = definition.getMaterials();
    this.materials = materialPattern.getPossibleBlocks();
    this.destructionRequired = definition.getDestructionRequired();

    this.blockRegion = FiniteBlockRegion.fromWorld(
        definition.getRegion(),
        match.getWorld(),
        this.materialPattern,
        match.getMap().getProto());
    if (this.blockRegion.getBlockVolume() == 0) {
      match.getLogger().warning("No destroyable blocks found in destroyable " + this.getName());
    }

    BlockDropsMatchModule bdmm = match.getModule(BlockDropsMatchModule.class);
    if (bdmm != null) {
      this.blockDropsRuleSet = bdmm.getRuleSet().subsetAffecting(this.blockRegion);
    }

    this.recalculateHealth();
    this.isShared = match.getCompetitors().stream().filter(this::canComplete).count() != 1;
  }

  // Remove @Nullable
  @Override
  public @NotNull Team getOwner() {
    Team owner = super.getOwner();
    if (owner == null) {
      throw new IllegalStateException("destroyable " + getId() + " has no owner");
    }
    return owner;
  }

  @Override
  public boolean getDeferTouches() {
    return true;
  }

  @Override
  public Component getTouchMessage(@Nullable ParticipantState toucher, boolean self) {
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
      proximityLocations = Collections.singleton(getBlockRegion()
          .getBounds()
          .getCenterPoint()
          .toLocation(getOwner().getMatch().getWorld()));
    }
    return proximityLocations;
  }

  @Override
  public boolean isAffectedBy(Mode mode) {
    return mode.getMaterialData() != null
        && (this.definition.getModes() == null || this.definition.getModes().contains(mode));
  }

  /** Calculate maximum/current health */
  protected void recalculateHealth() {
    // We only need blockMaterialHealth if there are destroyable blocks that are
    // replaced by other destroyable blocks when broken.
    if (this.isAffectedByBlockReplacementRules()) {
      this.blockMaterialHealth = new HashMap<>();
      this.buildMaterialHealthMap();
    } else {
      this.blockMaterialHealth = null;
      this.maxHealth = this.blockRegion.getBlockVolume();
      this.health = 0;
      for (Block block : this.blockRegion.getBlocks(match.getWorld())) {
        if (this.hasMaterial(MaterialData.block(block.getState()))) {
          this.health++;
        }
      }
    }
  }

  protected boolean isAffectedByBlockReplacementRules() {
    if (this.blockDropsRuleSet == null || this.blockDropsRuleSet.getRules().isEmpty()) {
      return false;
    }

    for (Block block : this.blockRegion.getBlocks(match.getWorld())) {
      for (BlockMaterialData material : this.materials) {
        BlockDrops drops = this.blockDropsRuleSet.getDrops(block.getState(), material);
        if (drops != null && drops.replacement != null && this.hasMaterial(drops.replacement)) {
          return true;
        }
      }
    }

    return false;
  }

  /** Used internally to break out of the below recursive algorithm when a cycle is detected */
  protected static final class Indestructible extends Exception {}

  protected void buildMaterialHealthMap() {
    this.maxHealth = 0;
    this.health = 0;
    Set<BlockMaterialData> visited = new HashSet<>();
    try {
      for (Block block : blockRegion.getBlocks(match.getWorld())) {
        Map<BlockMaterialData, Integer> materialHealthMap = new HashMap<>();
        int blockMaxHealth = 0;

        for (BlockMaterialData material : this.materials) {
          visited.clear();
          int blockHealth =
              this.buildBlockMaterialHealthMap(block, material, materialHealthMap, visited);
          if (blockHealth > blockMaxHealth) {
            blockMaxHealth = blockHealth;
          }
        }

        this.blockMaterialHealth.put(
            block.getLocation().toVector().toBlockVector(), materialHealthMap);
        this.maxHealth += blockMaxHealth;
        this.health += this.getBlockHealth(block.getState());
      }
    } catch (Indestructible ex) {
      this.health = this.maxHealth = Integer.MAX_VALUE;
      PGM.get()
          .getLogger()
          .warning("Destroyable "
              + this.getName()
              + " is indestructible due to block replacement cycle");
    }
  }

  protected int buildBlockMaterialHealthMap(
      Block block,
      BlockMaterialData material,
      Map<BlockMaterialData, Integer> materialHealthMap,
      Set<BlockMaterialData> visited)
      throws Indestructible {

    if (!this.hasMaterial(material)) {
      return 0;
    }

    Integer healthBoxed = materialHealthMap.get(material);
    if (healthBoxed != null) {
      return healthBoxed;
    }

    if (visited.contains(material)) {
      throw new Indestructible();
    }
    visited.add(material);

    int health = 1;
    if (this.blockDropsRuleSet != null) {
      BlockDrops drops = this.blockDropsRuleSet.getDrops(block.getState(), material);
      if (drops != null && drops.replacement != null) {
        health +=
            this.buildBlockMaterialHealthMap(block, drops.replacement, materialHealthMap, visited);
      }
    }

    materialHealthMap.put(material, health);
    return health;
  }

  /** Return the number of breaks required to change the given block to a non-objective world */
  public int getBlockHealth(BlockState blockState) {
    if (!this.getBlockRegion().contains(blockState)) {
      return 0;
    }

    if (this.blockMaterialHealth == null) {
      return this.hasMaterial(MaterialData.block(blockState)) ? 1 : 0;
    } else {
      Map<BlockMaterialData, Integer> materialHealthMap =
          this.blockMaterialHealth.get(blockState.getLocation().toVector().toBlockVector());
      if (materialHealthMap == null) {
        return 0;
      }
      Integer health = materialHealthMap.get(MaterialData.block(blockState));
      return health == null ? 0 : health;
    }
  }

  public int getBlockHealthChange(BlockState oldState, BlockState newState) {
    return this.getBlockHealth(newState) - this.getBlockHealth(oldState);
  }

  /**
   * Update the state of this Destroyable to reflect the given block being changed by the given
   * player.
   *
   * @param oldState State of the block before the change
   * @param newState State of the block after the change
   * @param player Player responsible for the change
   * @return An object containing information about the change, including the health delta, or null
   *     if this Destroyable was not affected by the block change
   */
  public DestroyableHealthChange handleBlockChange(
      BlockState oldState, BlockState newState, @Nullable ParticipantState player) {
    if (this.isDestroyed() || !this.getBlockRegion().contains(oldState)) return null;

    int deltaHealth = this.getBlockHealthChange(oldState, newState);
    if (deltaHealth == 0) return null;

    this.addHealth(deltaHealth);

    DestroyableHealthChange changeInfo =
        new DestroyableHealthChange(oldState, newState, player, deltaHealth);
    this.events.add(changeInfo);

    if (deltaHealth < 0) {
      touch(player);

      if (this.definition.isSparksActive()) {
        Location blockLocation = BlockVectors.center(oldState);
        Instant now = Instant.now();

        // Probability of a spark is time_since_last_spark / cooldown_time
        float chance = this.lastSparkTime == null
            ? 1.0f
            : ((float) (now.toEpochMilli() - this.lastSparkTime.toEpochMilli())
                / (float) SPARK_COOLDOWN.toMillis());
        if (this.match.getRandom().nextFloat() < chance) {
          this.lastSparkTime = now;

          // Spawn a firework where the block was
          if (PGM.get().getConfiguration().showFireworks()) {
            Firework firework = FireworkMatchModule.spawnFirework(
                blockLocation,
                FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withFlicker()
                    .withColor(this.getOwner().getFullColor())
                    .build(),
                0);

            NMS_HACKS.skipFireworksLaunch(firework);
          }

          if (this.definition.isSparksAll()) {
            // Players more than 64m away will not see or hear the fireworks, so play sound for them
            for (MatchPlayer listener : this.getOwner().getMatch().getPlayers()) {
              if (listener.getBukkit().getLocation().distance(blockLocation) > 64) {
                listener.playSound(Sounds.OBJECTIVE_FIREWORKS_FAR);
                listener.playSound(Sounds.OBJECTIVE_FIREWORKS_TWINKLE);
              }
            }
          }
        }
      }
    }

    this.match.callEvent(new DestroyableHealthChangeEvent(this.getMatch(), this, changeInfo));
    this.match.callEvent(new GoalStatusChangeEvent(this.getMatch(), this));

    if (this.isDestroyed()) {
      this.match.callEvent(new DestroyableDestroyedEvent(this.match, this));
      this.match.callEvent(new GoalCompleteEvent(
          this.getMatch(), this, this.getOwner(), false, this.getContributions()));
    }

    return changeInfo;
  }

  /**
   * Test if the given block change is allowed by this Destroyable
   *
   * @param oldState State of the block before the change
   * @param newState State of the block after the change
   * @param player Player responsible for the change
   * @return A player-readable message explaining why the block change is not allowed, or null if it
   *     is allowed
   */
  public String testBlockChange(
      BlockState oldState, BlockState newState, @Nullable ParticipantState player) {
    if (this.isDestroyed() || !this.getBlockRegion().contains(oldState)) return null;

    int deltaHealth = this.getBlockHealthChange(oldState, newState);
    if (deltaHealth == 0) return null;

    if (deltaHealth < 0) {
      // Damage
      if (player != null && player.getParty() == this.getOwner()) {
        return "objective.damageOwn";
      }
    } else if (deltaHealth > 0) {
      // Repair
      if (player != null && player.getParty() != this.getOwner()) {
        return "objective.repairOther";
      } else if (!this.definition.isRepairable()) {
        return "objective.repairDisabled";
      }
    }

    return null;
  }

  public FiniteBlockRegion getBlockRegion() {
    return this.blockRegion;
  }

  public boolean hasMaterial(MaterialData data) {
    return materialPattern.matches(data);
  }

  public void addHealth(int delta) {
    this.health = Math.max(0, Math.min(this.maxHealth, this.health + delta));
  }

  public int getMaxHealth() {
    return this.maxHealth;
  }

  public int getHealth() {
    return this.health;
  }

  public float getHealthPercent() {
    return (float) this.health / this.maxHealth;
  }

  public int getBreaks() {
    return this.maxHealth - this.health;
  }

  public double getDestructionRequired() {
    return this.destructionRequired;
  }

  public String renderDestructionRequired() {
    return Math.round(this.destructionRequired * 100) + "%";
  }

  private int getBreaksRequired(double destructionRequired) {
    return (int) Math.round(this.maxHealth * destructionRequired);
  }

  public int getBreaksRequired() {
    return this.getBreaksRequired(this.destructionRequired);
  }

  public void setDestructionRequired(double destructionRequired) {
    if (this.destructionRequired != destructionRequired) {
      if (this.getBreaks() >= this.getBreaksRequired(destructionRequired)) {
        throw new IllegalArgumentException("Destroyable is already destroyed that much");
      } else if (destructionRequired > 1) {
        throw new IllegalArgumentException("Cannot require more than 100% destruction");
      }

      this.destructionRequired = destructionRequired;
      this.getOwner().getMatch().callEvent(new FeatureChangeEvent(this.getMatch(), this));
    }
  }

  public void setBreaksRequired(int breaks) {
    this.setDestructionRequired((double) breaks / this.getMaxHealth());
  }

  @Override
  public double getCompletion() {
    return Math.min(1, (double) this.getBreaks() / this.getBreaksRequired());
  }

  @Override
  public String renderCompletion() {
    return StringUtils.percentage(this.getCompletion());
  }

  @NotNull
  @Override
  public String renderPreciseCompletion() {
    return this.getBreaks() + "/" + this.getBreaksRequired();
  }

  @Override
  public Component renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (this.getShowProgress() || (viewer.isObserving() && this.getBreaksRequired() > 1)) {
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
  public boolean getShowProgress() {
    return this.definition.getShowProgress();
  }

  public boolean isDestroyed() {
    return this.getBreaks() >= this.getBreaksRequired();
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
    return this.isDestroyed();
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return this.isDestroyed() && this.canComplete(team);
  }

  public @NotNull List<DestroyableHealthChange> getEvents() {
    return ImmutableList.copyOf(this.events);
  }

  public @NotNull ImmutableList<DestroyableContribution> getContributions() {
    if (this.contributions != null) {
      return this.contributions;
    }

    Map<MatchPlayerState, Integer> playerDamage = new DefaultMapAdapter<>(new HashMap<>(), 0);

    int totalDamage = 0;
    for (DestroyableHealthChange change : this.events) {
      if (change.getHealthChange() < 0) {
        MatchPlayerState player = change.getPlayerCause();
        if (player != null) {
          playerDamage.put(player, playerDamage.get(player) - change.getHealthChange());
        }
        totalDamage -= change.getHealthChange();
      }
    }

    ImmutableList.Builder<DestroyableContribution> builder = ImmutableList.builder();
    for (Map.Entry<MatchPlayerState, Integer> entry : playerDamage.entrySet()) {
      builder.add(new DestroyableContribution(
          entry.getKey(), (double) entry.getValue() / totalDamage, entry.getValue()));
    }

    ImmutableList<DestroyableContribution> contributions = builder.build();
    if (this.isDestroyed()) {
      // Only cache if completely destroyed
      this.contributions = contributions;
    }
    return contributions;
  }

  @Override
  public void replaceBlocks(BlockMaterialData newMaterial) {
    // Calling this method causes all non-destroyed blocks to be replaced, and the world
    // list to be replaced with one containing only the new block. If called on a multi-stage
    // destroyable, i.e. one which is affected by block replacement rules, it effectively ceases
    // to be multi-stage. Even if there are block replacement rules for the new block, the
    // replacements will not be in the world list, and so those blocks will be considered
    // destroyed the first time they are mined. This can have some strange effects on the health
    // of the destroyable: individual block health can only decrease, while the total health
    // percentage can only increase.

    for (Block block : this.getBlockRegion().getBlocks(match.getWorld())) {
      BlockState oldState = block.getState();
      int oldHealth = this.getBlockHealth(oldState);

      if (oldHealth > 0) {
        newMaterial.applyTo(block, true);
      }
    }

    // Update the world list on switch
    this.materialPattern = newMaterial.toMatcher();
    this.materials = Set.of(newMaterial);

    // If there is a block health map, get rid of it, since there is now only one world in the list
    this.blockMaterialHealth = null;

    this.recalculateHealth();
  }

  @Override
  public boolean isObjectiveMaterial(Block block) {
    return this.hasMaterial(MaterialData.block(block));
  }

  @Override
  public String getModeChangeMessage(Material material) {
    return ModeUtils.formatMaterial(material) + " OBJECTIVE MODE";
  }
}

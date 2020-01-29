package tc.oc.pgm.destroyable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.block.BlockVectors;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.named.NameStyle;
import tc.oc.pgm.Config;
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
import tc.oc.pgm.goals.IncrementalGoal;
import tc.oc.pgm.goals.ModeChangeGoal;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.modes.ModeUtils;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.teams.Team;
import tc.oc.util.StringUtils;
import tc.oc.util.collection.DefaultMapAdapter;
import tc.oc.util.components.Components;
import tc.oc.world.NMSHacks;

public class Destroyable extends TouchableGoal<DestroyableFactory>
    implements IncrementalGoal<DestroyableFactory>, ModeChangeGoal<DestroyableFactory> {

  // Block replacement rules in this ruleset will be used to calculate destroyable health
  protected BlockDropsRuleSet blockDropsRuleSet;

  protected final FiniteBlockRegion blockRegion;
  protected final Set<SingleMaterialMatcher> materialPatterns = new HashSet<>();
  protected final Set<MaterialData> materials = new HashSet<>();

  // The percentage of blocks that must be broken for the entire Destroyable to be destroyed.
  protected double destructionRequired;

  protected final Duration SPARK_COOLDOWN = Duration.millis(75);
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
  protected Map<BlockVector, Map<MaterialData, Integer>> blockMaterialHealth;

  protected final List<DestroyableHealthChange> events = Lists.newArrayList();
  protected ImmutableList<DestroyableContribution> contributions;

  protected Iterable<Location> proximityLocations;

  public Destroyable(DestroyableFactory definition, Match match) {
    super(definition, match);

    for (SingleMaterialMatcher pattern : definition.getMaterials()) {
      addMaterials(pattern);
    }

    this.destructionRequired = definition.getDestructionRequired();

    this.blockRegion =
        FiniteBlockRegion.fromWorld(
            definition.getRegion(),
            match.getWorld(),
            this.materialPatterns,
            match.getMap().getProto());
    if (this.blockRegion.getBlocks().isEmpty()) {
      match.getLogger().warning("No destroyable blocks found in destroyable " + this.getName());
    }

    BlockDropsMatchModule bdmm = match.getModule(BlockDropsMatchModule.class);
    if (bdmm != null) {
      this.blockDropsRuleSet =
          bdmm.getRuleSet().subsetAffecting(this.materials).subsetAffecting(this.blockRegion);
    }

    this.recalculateHealth();
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
          "match.touch.destroyable.owner",
          Components.blank(),
          getComponentName(),
          getOwner().getComponentName());
    } else if (self) {
      return new PersonalizedTranslatable(
          "match.touch.destroyable.owner.you",
          Components.blank(),
          getComponentName(),
          getOwner().getComponentName());
    } else {
      return new PersonalizedTranslatable(
          "match.touch.destroyable.owner.toucher",
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
              getBlockRegion()
                  .getBounds()
                  .getCenterPoint()
                  .toLocation(getOwner().getMatch().getWorld()));
    }
    return proximityLocations;
  }

  void addMaterials(SingleMaterialMatcher pattern) {
    materialPatterns.add(pattern);
    if (pattern.dataMatters()) {
      materials.add(pattern.getMaterialData());
    } else {
      // Hacky, but there is no other simple way to deal with block replacement
      materials.addAll(NMSHacks.getBlockStates(pattern.getMaterial()));
    }
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
      this.maxHealth = this.blockRegion.getBlocks().size();
      this.health = 0;
      for (Block block : this.blockRegion.getBlocks()) {
        if (this.hasMaterial(block.getState().getData())) {
          this.health++;
        }
      }
    }
  }

  protected boolean isAffectedByBlockReplacementRules() {
    if (this.blockDropsRuleSet == null || this.blockDropsRuleSet.getRules().isEmpty()) {
      return false;
    }

    for (Block block : this.blockRegion.getBlocks()) {
      for (MaterialData material : this.materials) {
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
    Set<MaterialData> visited = new HashSet<>();
    try {
      for (Block block : blockRegion.getBlocks()) {
        Map<MaterialData, Integer> materialHealthMap = new HashMap<>();
        int blockMaxHealth = 0;

        for (MaterialData material : this.materials) {
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
          .warning(
              "Destroyable "
                  + this.getName()
                  + " is indestructible due to block replacement cycle");
    }
  }

  protected int buildBlockMaterialHealthMap(
      Block block,
      MaterialData material,
      Map<MaterialData, Integer> materialHealthMap,
      Set<MaterialData> visited)
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
      return this.hasMaterial(blockState.getData()) ? 1 : 0;
    } else {
      Map<MaterialData, Integer> materialHealthMap =
          this.blockMaterialHealth.get(blockState.getLocation().toVector().toBlockVector());
      if (materialHealthMap == null) {
        return 0;
      }
      Integer health = materialHealthMap.get(blockState.getData());
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

      if (this.definition.hasSparks()) {
        Location blockLocation = BlockVectors.center(oldState);
        Instant now = Instant.now();

        // Probability of a spark is time_since_last_spark / cooldown_time
        float chance =
            this.lastSparkTime == null
                ? 1.0f
                : ((float) (now.getMillis() - this.lastSparkTime.getMillis())
                    / (float) SPARK_COOLDOWN.getMillis());
        if (this.match.getRandom().nextFloat() < chance) {
          this.lastSparkTime = now;

          // Players more than 64m away will not see or hear the fireworks, so just play the sound
          // for them
          for (MatchPlayer listener : this.getOwner().getMatch().getPlayers()) {
            if (listener.getBukkit().getLocation().distance(blockLocation) > 64) {
              listener
                  .getBukkit()
                  .playSound(listener.getBukkit().getLocation(), Sound.FIREWORK_BLAST2, 0.75f, 1f);
              listener
                  .getBukkit()
                  .playSound(
                      listener.getBukkit().getLocation(), Sound.FIREWORK_TWINKLE2, 0.75f, 1f);
            }
          }
        }
      }
    }

    this.match.callEvent(new DestroyableHealthChangeEvent(this.getMatch(), this, changeInfo));
    this.match.callEvent(new GoalStatusChangeEvent(this.getMatch(), this));

    if (this.isDestroyed()) {
      this.match.callEvent(new DestroyableDestroyedEvent(this.match, this));
      this.match.callEvent(
          new GoalCompleteEvent(
              this.getMatch(), this, this.getOwner(), false, this.getContributions()));
    }

    return changeInfo;
  }

  @Override
  protected void playTouchEffects(ParticipantState toucher) {
    // We make our own touch sounds
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
        return "match.destroyable.damageOwn";
      }
    } else if (deltaHealth > 0) {
      // Repair
      if (player != null && player.getParty() != this.getOwner()) {
        return "match.destroyable.repairOther";
      } else if (!this.definition.isRepairable()) {
        return "match.destroyable.repairDisabled";
      }
    }

    return null;
  }

  public FiniteBlockRegion getBlockRegion() {
    return this.blockRegion;
  }

  public boolean hasMaterial(MaterialData data) {
    for (SingleMaterialMatcher material : materialPatterns) {
      if (material.matches(data)) return true;
    }
    return false;
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

  @Override
  public boolean isAffectedByModeChanges() {
    return this.definition.hasModeChanges();
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
    return (double) this.getBreaks() / this.getBreaksRequired();
  }

  @Override
  public String renderCompletion() {
    return StringUtils.percentage(this.getCompletion());
  }

  @Override
  public String renderPreciseCompletion() {
    return this.getBreaks() + "/" + this.getBreaksRequired();
  }

  @Override
  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (this.getShowProgress() || viewer.isObserving()) {
      String text = this.renderCompletion();
      if (Config.Scoreboard.preciseProgress()) {
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
  public boolean getShowProgress() {
    return this.definition.getShowProgress();
  }

  public boolean isDestroyed() {
    return this.getBreaks() >= this.getBreaksRequired();
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
    return this.isDestroyed();
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return this.isDestroyed() && this.canComplete(team);
  }

  public @Nonnull List<DestroyableHealthChange> getEvents() {
    return ImmutableList.copyOf(this.events);
  }

  public @Nonnull ImmutableList<DestroyableContribution> getContributions() {
    if (this.contributions != null) {
      return this.contributions;
    }

    Map<MatchPlayerState, Integer> playerDamage =
        new DefaultMapAdapter<>(new HashMap<MatchPlayerState, Integer>(), 0);

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
      builder.add(
          new DestroyableContribution(
              entry.getKey(), (double) entry.getValue() / totalDamage, entry.getValue()));
    }

    ImmutableList<DestroyableContribution> contributions = builder.build();
    if (this.isDestroyed()) {
      // Only cache if completely destroyed
      this.contributions = contributions;
    }
    return contributions;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void replaceBlocks(MaterialData newMaterial) {
    // Calling this method causes all non-destroyed blocks to be replaced, and the world
    // list to be replaced with one containing only the new block. If called on a multi-stage
    // destroyable, i.e. one which is affected by block replacement rules, it effectively ceases
    // to be multi-stage. Even if there are block replacement rules for the new block, the
    // replacements will not be in the world list, and so those blocks will be considered
    // destroyed the first time they are mined. This can have some strange effects on the health
    // of the destroyable: individual block health can only decrease, while the total health
    // percentage can only increase.

    for (Block block : this.getBlockRegion().getBlocks()) {
      BlockState oldState = block.getState();
      int oldHealth = this.getBlockHealth(oldState);

      if (oldHealth > 0) {
        block.setTypeIdAndData(newMaterial.getItemTypeId(), newMaterial.getData(), true);
      }
    }

    // Update the world list on switch
    this.materialPatterns.clear();
    this.materials.clear();
    addMaterials(new SingleMaterialMatcher(newMaterial));

    // If there is a block health map, get rid of it, since there is now only one world in the list
    this.blockMaterialHealth = null;

    this.recalculateHealth();
  }

  @Override
  public boolean isObjectiveMaterial(Block block) {
    return this.hasMaterial(block.getState().getData());
  }

  @Override
  public String getModeChangeMessage(Material material) {
    return ModeUtils.formatMaterial(material) + " OBJECTIVE MODE";
  }
}

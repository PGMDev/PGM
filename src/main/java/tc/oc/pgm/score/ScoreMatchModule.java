package tc.oc.pgm.score;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.joda.time.Instant;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.event.PlayerItemTransferEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.util.collection.DefaultMapAdapter;

@ListenerScope(MatchScope.RUNNING)
public class ScoreMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ScoreConfig config;
  private final Set<ScoreBox> scoreBoxes;
  private final Map<Competitor, Double> scores = new DefaultMapAdapter<>(new HashMap<>(), 0d);

  public ScoreMatchModule(Match match, ScoreConfig config, Set<ScoreBox> scoreBoxes) {
    this.match = match;
    this.config = config;
    this.scoreBoxes = scoreBoxes;
  }

  @Override
  public void load() {
    match.addVictoryCondition(new ScoreVictoryCondition());
  }

  public boolean hasScoreLimit() {
    return this.config.scoreLimit > 0;
  }

  public int getScoreLimit() {
    checkState(hasScoreLimit());
    return this.config.scoreLimit;
  }

  public double getScore(Competitor competitor) {
    return this.scores.get(competitor);
  }

  /** Gets the score message for the match. */
  public String getScoreMessage() {
    List<String> scores = Lists.newArrayList();
    for (Entry<Competitor, Double> scorePair : this.scores.entrySet()) {
      scores.add(scorePair.getKey().getColor().toString() + ((int) (double) scorePair.getValue()));
    }
    return ChatColor.DARK_AQUA + "Score: " + Joiner.on(" ").join(scores);
  }

  /** Gets the status message for the match. */
  public String getStatusMessage() {
    StringBuilder message = new StringBuilder(this.getScoreMessage());
    if (this.config.scoreLimit > 0) {
      message
          .append("  ")
          .append(ChatColor.GRAY)
          .append("[")
          .append(this.config.scoreLimit)
          .append("]");
    }
    return message.toString();
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void incrementDeath(MatchPlayerDeathEvent event) {
    if (!event.getVictim().isParticipating()) return;

    // add +1 to killer's team if it was a kill, otherwise -1 to victim's team
    if (event.isChallengeKill()) {
      this.incrementScore(event.getKiller().getParty(), this.config.killScore);
    } else {
      this.incrementScore(event.getVictim().getCompetitor(), -this.config.deathScore);
    }
  }

  private double redeemItems(ScoreBox box, ItemStack stack) {
    if (stack == null) return 0;
    double points = 0;
    for (Entry<SingleMaterialMatcher, Double> entry : box.getRedeemables().entrySet()) {
      if (entry.getKey().matches(stack.getData())) {
        points += entry.getValue() * stack.getAmount();
        stack.setAmount(0);
      }
    }
    return points;
  }

  private double redeemItems(ScoreBox box, ItemStack[] stacks) {
    double total = 0;
    for (int i = 0; i < stacks.length; i++) {
      double points = redeemItems(box, stacks[i]);
      if (points != 0) stacks[i] = null;
      total += points;
    }
    return total;
  }

  private double redeemItems(ScoreBox box, PlayerInventory inventory) {
    ItemStack[] notArmor = inventory.getContents();
    ItemStack[] armor = inventory.getArmorContents();

    double points = redeemItems(box, notArmor) + redeemItems(box, armor);

    if (points != 0) {
      inventory.setContents(notArmor);
      inventory.setArmorContents(armor);
    }

    return points;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void playerEnterBox(CoarsePlayerMoveEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract() || player.getBukkit().isDead()) return;

    ParticipantState playerState = player.getParticipantState();
    Vector from = event.getBlockFrom().toVector();
    Vector to = event.getBlockTo().toVector();

    for (ScoreBox box : this.scoreBoxes) {
      if (box.getRegion().enters(from, to) && box.canScore(playerState)) {
        if (box.isCoolingDown(playerState)) {
          match
              .getLogger()
              .warning(
                  playerState.getId()
                      + " tried to score multiple times in one second (from="
                      + from
                      + " to="
                      + to
                      + ")");
        } else {
          this.playerScore(box, player, box.getScore() + redeemItems(box, player.getInventory()));
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void playerAcquireRedeemableInBox(final PlayerItemTransferEvent event) {
    if (!event.isAcquiring()) return;
    final MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract() || player.getBukkit().isDead()) return;

    for (final ScoreBox box : this.scoreBoxes) {
      if (!box.getRedeemables().isEmpty()
          && box.getRegion().contains(player.getBukkit())
          && box.canScore(player.getParticipantState())) {
        match
            .getScheduler(MatchScope.RUNNING)
            .runTask(
                new Runnable() {
                  @Override
                  public void run() {
                    if (player.getBukkit().isOnline()) {
                      double points = redeemItems(box, player.getInventory());
                      ScoreMatchModule.this.playerScore(box, player, points);
                    }
                  }
                });
      }
    }
  }

  private void playerScore(ScoreBox box, MatchPlayer player, double points) {
    checkState(player.isParticipating());

    if (points == 0) return;

    this.incrementScore(player.getCompetitor(), points);
    box.setLastScoreTime(player.getState(), Instant.now());

    int wholePoints = (int) points;
    if (wholePoints < 1) return;

    match.sendMessage(
        new PersonalizedTranslatable(
            "match.score.scorebox",
            player.getStyledName(NameStyle.COLOR),
            new PersonalizedTranslatable(
                wholePoints == 1 ? "points.singularCompound" : "points.pluralCompound",
                ChatColor.DARK_AQUA + String.valueOf(wholePoints) + ChatColor.GRAY),
            player.getParty().getStyledName(NameStyle.COLOR)));
    player.playSound(new Sound("random.levelup"));
  }

  public void incrementScore(Competitor competitor, double amount) {
    double oldScore = this.scores.get(competitor);
    double newScore = oldScore + amount;

    if (this.config.scoreLimit > 0 && newScore > this.config.scoreLimit) {
      newScore = this.config.scoreLimit;
    }

    CompetitorScoreChangeEvent event =
        new CompetitorScoreChangeEvent(competitor, oldScore, newScore);
    this.match.callEvent(event);

    this.scores.put(competitor, event.getNewScore());

    this.match.calculateVictory();
  }
}

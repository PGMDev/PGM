package tc.oc.pgm.score;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@ListenerScope(MatchScope.RUNNING)
public class ScoreMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ScoreConfig config;
  private final Set<ScoreBox> scoreBoxes;
  private final Map<UUID, Double> contributions = new DefaultMapAdapter<>(new HashMap<>(), 0d);
  private final Map<Competitor, Double> scores = new DefaultMapAdapter<>(new HashMap<>(), 0d);
  private MercyRule mercyRule;

  public ScoreMatchModule(Match match, ScoreConfig config, Set<ScoreBox> scoreBoxes) {
    this.match = match;
    this.config = config;
    this.scoreBoxes = scoreBoxes;
    this.match.getCompetitors().forEach(competitor -> this.scores.put(competitor, 0.0));

    if (this.config.mercyLimit() > 0) {
      this.mercyRule =
          new MercyRule(this, config.scoreLimit(), config.mercyLimit(), config.mercyLimitMin());
    }
  }

  @Override
  public void load() {
    match.addVictoryCondition(new ScoreVictoryCondition());
  }

  public ScoreConfig.Display getDisplay() {
    return config.display();
  }

  public boolean hasScoreLimit() {
    return this.config.scoreLimit() > 0 || hasMercyRule();
  }

  public boolean hasMercyRule() {
    return this.mercyRule != null;
  }

  public Filter getScoreboardFilter() {
    return this.config.scoreboardFilter();
  }

  public int getScoreLimit() {
    assertTrue(hasScoreLimit());

    if (hasMercyRule()) {
      return this.mercyRule.getScoreLimit();
    }

    return this.config.scoreLimit();
  }

  public Map<Competitor, Double> getScores() {
    return this.scores;
  }

  public double getScore(@NotNull Competitor competitor) {
    return this.scores.get(competitor);
  }

  /** Gets the score message for the match. */
  public Component getScoreMessage(MatchPlayer matchPlayer) {
    List<Component> scoreMessages = Lists.newArrayList();
    final FreeForAllMatchModule ffamm = match.getModule(FreeForAllMatchModule.class);
    if (ffamm != null) {
      scoreMessages = this.scores.entrySet().stream()
          .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
          .limit(10)
          .map(x -> text()
              .append(x.getKey().getName(NameStyle.VERBOSE))
              .append(text(": ", NamedTextColor.GRAY))
              .append(text((int) x.getValue().doubleValue()))
              .color(NamedTextColor.WHITE)
              .build())
          .collect(Collectors.toList());
    } else {

      for (Entry<Competitor, Double> scorePair : this.scores.entrySet()) {
        scoreMessages.add(text(
            ((int) scorePair.getValue().doubleValue()),
            TextFormatter.convert(scorePair.getKey().getColor())));
      }
    }
    TextComponent returnMessage = text()
        .append(translatable("match.info.score").color(NamedTextColor.DARK_AQUA))
        .append(text(": ", NamedTextColor.DARK_AQUA))
        .append(TextFormatter.list(scoreMessages, NamedTextColor.GRAY))
        .build();
    if (matchPlayer != null && matchPlayer.getCompetitor() != null && ffamm != null) {
      returnMessage = returnMessage.append(text()
          .color(NamedTextColor.GRAY)
          .append(text(" | ", NamedTextColor.GRAY))
          .append(translatable("match.info.you"))
          .append(text(": "))
          .color(TextFormatter.convert(matchPlayer.getCompetitor().getColor()))
          .append(text(
              (int) scores.get(matchPlayer.getCompetitor()).doubleValue(), NamedTextColor.WHITE))
          .build());
    }
    return returnMessage;
  }

  /** Gets the status message for the match. */
  public Component getStatusMessage(MatchPlayer matchPlayer) {
    Component message = this.getScoreMessage(matchPlayer);

    if (this.config.scoreLimit() > 0) {
      message = message.append(text("  [" + this.config.scoreLimit() + "]", NamedTextColor.GRAY));
    }
    return message;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void incrementDeath(MatchPlayerDeathEvent event) {
    if (!event.getVictim().isParticipating()) return;

    // add +1 to killer's team if it was a kill, otherwise -1 to victim's team
    if (event.isChallengeKill()) {
      this.incrementScore(
          event.getKiller().getId(), event.getKiller().getParty(), this.config.killScore());
    } else {
      this.incrementScore(
          event.getVictim().getId(), event.getVictim().getCompetitor(), -this.config.deathScore());
    }
  }

  private double redeemItems(ScoreBox box, ItemStack stack) {
    if (stack == null) return 0;
    double points = 0;
    for (Entry<MaterialMatcher, Double> entry : box.getRedeemables().entrySet()) {
      if (entry.getKey().matches(stack)) {
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
  public void playerEnterBox(PlayerCoarseMoveEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract() || player.getBukkit().isDead()) return;

    Vector from = event.getBlockFrom().toVector();
    Vector to = event.getBlockTo().toVector();

    for (ScoreBox box : this.scoreBoxes) {
      if (box.getRegion().enters(from, to) && box.canScore(player)) {
        if (box.isCoolingDown(player)) {
          match
              .getLogger()
              .warning("%s tried to score multiple times in under a second (from=%s to=%s)"
                  .formatted(player.getId(), from, to));
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
          && box.canScore(player)) {
        match.getExecutor(MatchScope.RUNNING).execute(() -> {
          if (player.getBukkit().isOnline()) {
            double points = redeemItems(box, player.getInventory());
            ScoreMatchModule.this.playerScore(box, player, points);
          }
        });
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void handleJoin(final PlayerParticipationStartEvent event) {
    double contribution = contributions.get(event.getPlayer().getId());
    if (contribution <= PGM.get().getConfiguration().getGriefScore()) {
      event.cancel(translatable("join.err.teamGrief", NamedTextColor.RED));
    }
  }

  private void playerScore(ScoreBox box, MatchPlayer player, double points) {
    assertTrue(player.isParticipating());

    if (points == 0) return;

    this.incrementScore(player.getId(), player.getCompetitor(), points);
    box.setLastScoreTime(player, Instant.now());

    int wholePoints = (int) points;
    if (wholePoints < 1 || box.isSilent()) return;

    match.sendMessage(translatable(
        "scorebox.scored",
        player.getName(NameStyle.COLOR),
        translatable(
            wholePoints == 1 ? "misc.point" : "misc.points",
            text(wholePoints, NamedTextColor.DARK_AQUA)),
        player.getParty().getName()));
    player.playSound(Sounds.SCORE);
  }

  public void incrementScore(UUID player, Competitor competitor, double amount) {
    double contribution = contributions.get(player) + amount;
    contributions.put(player, contribution);
    incrementScore(competitor, amount);

    MatchPlayer mp = match.getPlayer(player);
    if (mp == null) return;

    match.callEvent(new MatchPlayerScoreEvent(mp, amount));

    if (contribution <= PGM.get().getConfiguration().getGriefScore()) {
      // wait until the next tick to do this so stat recording and other stuff works
      match.getExecutor(MatchScope.RUNNING).execute(() -> {
        if (mp.getParty() instanceof Competitor) {
          match.setParty(mp, match.getDefaultParty());
          mp.sendWarning(translatable("join.err.teamGrief", NamedTextColor.RED));
        }
      });
    }
  }

  public void setScore(@NotNull Competitor competitor, double value) {
    double curr = getScore(competitor);
    if (curr != value) setScore(competitor, curr, value);
  }

  public void incrementScore(Competitor competitor, double amount) {
    double oldScore = this.scores.get(competitor);
    double newScore = oldScore + amount;
    setScore(competitor, oldScore, newScore);
  }

  private void setScore(Competitor competitor, double oldScore, double newScore) {
    if (this.config.scoreLimit() > 0 && newScore > this.config.scoreLimit()) {
      newScore = this.config.scoreLimit();
    }

    CompetitorScoreChangeEvent event =
        new CompetitorScoreChangeEvent(competitor, oldScore, newScore);
    this.match.callEvent(event);

    this.scores.put(competitor, event.getNewScore());

    if (hasMercyRule()) {
      this.mercyRule.handleEvent(event);
    }

    this.match.calculateVictory();
  }

  public double getContribution(UUID player) {
    return contributions.get(player);
  }
}

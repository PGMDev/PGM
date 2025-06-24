package tc.oc.pgm.scoreboard;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.wool.MonumentWool;

class SidebarRenderer {

  public static final int MAX_ROWS = 16; // Max rows on the scoreboard
  public static final int MAX_LENGTH = 30; // Max characters per line allowed
  public static final int MAX_TITLE = 32; // Max characters allowed in title

  private final Match match;
  private final SidebarMatchModule smm;

  public SidebarRenderer(Match match, SidebarMatchModule smm) {
    this.match = match;
    this.smm = smm;
  }

  public Component renderTitle() {
    final MapInfo map = match.getMap();
    final Component header = PGM.get().getConfiguration().getMatchHeader();
    if (header != null) {
      return header.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Component gamemode = map.getGamemode();
    if (gamemode != null) {
      return gamemode.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Collection<Gamemode> gamemodes = map.getGamemodes();
    if (!gamemodes.isEmpty()) {
      boolean acronyms = gamemodes.size() > 1;
      List<Component> gmComponents = gamemodes.stream()
          .map(gm -> text(acronyms ? gm.getAcronym() : gm.getFullName()))
          .collect(Collectors.toList());
      return TextFormatter.list(gmComponents, NamedTextColor.AQUA);
    }

    final List<Component> games = new LinkedList<>();

    // First, find a primary game mode
    for (final MapTag tag : map.getTags()) {
      if (!tag.isGamemode() || tag.isAuxiliary()) continue;

      if (games.isEmpty()) {
        games.add(tag.getName().color(NamedTextColor.AQUA));
        continue;
      }

      // When there are multiple, primary game modes
      games.set(0, translatable("gamemode.generic.name", NamedTextColor.AQUA));
      break;
    }

    // Second, append auxiliary game modes
    for (final MapTag tag : map.getTags()) {
      if (!tag.isGamemode() || !tag.isAuxiliary()) continue;

      // There can only be 2 game modes
      if (games.size() < 2) {
        games.add(tag.getName().color(NamedTextColor.AQUA));
      } else {
        break;
      }
    }

    // Display "Blitz: Rage" rather than "Blitz and Rage"
    if (games.size() == 2
        && Stream.of("blitz", "rage")
            .allMatch(id -> map.getTags().stream().anyMatch(mt -> mt.getId().equals(id)))) {
      games.clear();
      games.add(text(Gamemode.BLITZ_RAGE.getFullName(), NamedTextColor.AQUA));
    }

    return TextFormatter.list(games, NamedTextColor.AQUA);
  }

  public List<Component> renderSidebar(final Party party) {
    RenderContext context = new RenderContext(match, party);

    // Scores/Blitz
    renderScoresOrBlitz(context);

    // Shared goals i.e. not grouped under a specific team
    renderSharedGoals(context);

    // Team-specific goals
    for (Competitor competitor : getSortedCompetitors(context, party)) {
      // Avoid rendering team name & objectives if no objective will fit after the name
      if ((context.size() + 2) >= MAX_ROWS) break;

      renderCompetitor(context, competitor);
    }

    // Config-based footer, if any is defined
    renderFooter(context);

    return context.getResult();
  }

  private void renderScoresOrBlitz(RenderContext context) {
    if (!context.hasScores && !context.isBlitz) return;
    context.startSection();

    if (context.hasScores) {
      var smm = assertNotNull(context.smm);
      int limit = smm.hasScoreLimit() ? smm.getScoreLimit() : -1;

      for (Competitor competitor : match.getSortedCompetitors()) {
        if (!smm.getScoreboardFilter().response(competitor)) continue;
        context.addRow(context.display.format(competitor, (int) smm.getScore(competitor), limit));

        // No point rendering more scores, usually seen in FFA
        if (context.isFull()) break;
      }
    } else {
      for (Competitor competitor : match.getSortedCompetitors()) {
        Component text = renderBlitz(competitor);
        if (text == null) continue;
        if (text != empty()) text = text.append(space());
        context.addRow(text.append(competitor.getName(NameStyle.SIMPLE_COLOR)));

        // No point rendering more scores, usually seen in FFA
        if (context.isFull()) break;
      }
    }
  }

  private void renderSharedGoals(RenderContext context) {
    context.startSection();
    for (Goal<?> goal : context.sharedGoals) {
      context.addRow(this.renderGoal(goal, null, context.viewer));
    }
  }

  private List<Competitor> getSortedCompetitors(RenderContext context, Party viewer) {
    List<Competitor> sortedCompetitors = new ArrayList<>(match.getSortedCompetitors());
    sortedCompetitors.retainAll(context.competitorGoals.keySet());
    // Bump viewing party to the top of the list
    if (viewer instanceof Competitor && sortedCompetitors.remove(viewer)) {
      sortedCompetitors.add(0, (Competitor) viewer);
    }
    return sortedCompetitors;
  }

  private void renderCompetitor(RenderContext context, Competitor competitor) {
    if (!context.isSuperCompact) context.startSection();

    // Add a row for the team name
    context.addRow(competitor.getName());

    if (context.isCompactWool) {
      boolean firstWool = true;

      List<Goal<?>> sortedWools = context.competitorGoals.get(competitor);
      sortedWools.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

      // Calculate whether having three spaces between each wool would fit on the scoreboard.
      boolean horizontalCompact =
          MAX_LENGTH < (3 * sortedWools.size()) + (3 * (sortedWools.size() - 1)) + 1;
      TextComponent.Builder woolText = text();
      for (Goal<?> goal : sortedWools) {
        if (goal instanceof MonumentWool wool) {
          TextComponent spacer = space();
          if (!firstWool && !horizontalCompact) {
            spacer = spacer.append(space()).append(space());
          }
          firstWool = false;
          woolText.append(spacer
              .append(wool.renderSidebarStatusText(competitor, context.viewer))
              .color(wool.renderSidebarStatusColor(competitor, context.viewer)));
        }
      }
      // Add a row for the compact wools
      context.addRow(woolText.build());

    } else {
      // Not compact; add a row for each of this team's goals
      for (Goal<?> goal : context.competitorGoals.get(competitor)) {
        context.addRow(this.renderGoal(goal, competitor, context.viewer));
      }
    }
  }

  private void renderFooter(RenderContext context) {
    final Component footer = PGM.get().getConfiguration().getMatchFooter();
    if (footer != null) {
      // Only shows footer if there are one or two rows available
      if (context.size() < MAX_ROWS - 2) {
        context.startSection();
      }
      context.addRow(footer);
    }
  }

  private Component renderBlitz(Competitor competitor) {
    BlitzMatchModule bmm = match.needModule(BlitzMatchModule.class);
    if (!bmm.getConfig().getScoreboardFilter().response(competitor)) {
      return null;
    } else if (competitor instanceof Team) {
      return text(bmm.getRemainingPlayers(competitor), NamedTextColor.WHITE);
    } else if (competitor instanceof Tribute && bmm.getConfig().getNumLives() > 1) {
      final UUID id = competitor.getPlayers().iterator().next().getId();
      return text(bmm.getNumOfLives(id), NamedTextColor.WHITE);
    } else {
      return empty();
    }
  }

  private Component renderGoal(Goal<?> goal, @Nullable Competitor competitor, Party viewingParty) {
    final SidebarMatchModule.BlinkTask blinkTask = smm.blinkingGoals.get(goal);
    final TextComponent.Builder line = text();

    line.append(space());
    line.append(goal.renderSidebarStatusText(competitor, viewingParty)
        .color(
            blinkTask != null && blinkTask.isDark()
                ? NamedTextColor.BLACK
                : goal.renderSidebarStatusColor(competitor, viewingParty)));

    if (goal instanceof ProximityGoal) {
      final ProximityGoal<?> proximity = (ProximityGoal<?>) goal;
      if (proximity.shouldShowProximity(competitor, viewingParty)) {
        line.append(space());
        line.append(proximity.renderProximity(competitor, viewingParty));
      }
    }

    line.append(space());
    line.append(goal.renderSidebarLabelText(competitor, viewingParty)
        .color(goal.renderSidebarLabelColor(competitor, viewingParty)));

    return line.build();
  }

  @SuppressWarnings("deprecation")
  public String renderTitle(Component title, MatchPlayer viewer) {
    return StringUtils.substring(
        TextTranslations.translateLegacy(title, viewer), 0, SidebarRenderer.MAX_TITLE);
  }

  @SuppressWarnings("deprecation")
  public String renderRow(Component row, MatchPlayer viewer) {
    return StringUtils.substring(
        TextTranslations.translateLegacy(row, viewer), 0, SidebarRenderer.MAX_LENGTH);
  }
}

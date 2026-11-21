package tc.oc.pgm.goals;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public final class GoalFormatter {
  private static final double MIN_CREDIT_SHARE = 0.2;

  private GoalFormatter() {}

  public static Component formatContributions(Collection<? extends Contribution> contributions) {
    return formatContributions(
        contributions, entry -> entry.getPlayerState().getName(NameStyle.COLOR));
  }

  public static Component formatContributionsWithShares(
      Collection<? extends Contribution> contributions) {
    return formatContributions(
        contributions,
        entry -> translatable(
            "objective.credit.percentage",
            entry.getPlayerState().getName(NameStyle.COLOR),
            text(Math.round(entry.getPercentage() * 100), NamedTextColor.AQUA)));
  }

  private static Component formatContributions(
      Collection<? extends Contribution> contributions,
      Function<Contribution, Component> entryFormat) {
    List<? extends Contribution> sorted = new ArrayList<>(contributions);
    sorted.sort(Comparator.comparingDouble(Contribution::getPercentage).reversed());

    List<Component> contributors = new ArrayList<>();
    boolean someExcluded = false;
    for (Contribution entry : sorted) {
      if (entry.getPercentage() > MIN_CREDIT_SHARE) {
        contributors.add(entryFormat.apply(entry));
      } else {
        someExcluded = true;
      }
    }

    if (contributors.isEmpty()) {
      return translatable(someExcluded ? "objective.credit.many" : "objective.credit.unknown");
    }
    if (someExcluded) {
      contributors.add(translatable("objective.credit.etc"));
    }
    return TextFormatter.list(contributors, NamedTextColor.WHITE);
  }
}

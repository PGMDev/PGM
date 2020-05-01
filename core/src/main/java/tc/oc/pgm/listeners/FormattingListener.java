package tc.oc.pgm.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.destroyable.DestroyableDestroyedEvent;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.translations.TranslationUtils;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

public class FormattingListener implements Listener {
  @EventHandler(priority = EventPriority.MONITOR)
  public void playerWoolPlace(final PlayerWoolPlaceEvent event) {
    if (!event.getWool().isVisible()) return;

    event
        .getMatch()
        .sendMessage(
            new PersonalizedTranslatable(
                "wool.complete.owned",
                event.getPlayer().getStyledName(NameStyle.COLOR),
                event.getWool().getComponentName(),
                event.getPlayer().getParty().getComponentName()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void coreLeak(final CoreLeakEvent event) {
    if (!event.getCore().isVisible()) return;

    event
        .getMatch()
        .sendMessage(
            new PersonalizedText(
                new PersonalizedTranslatable(
                    "core.complete.owned",
                    formatContributions(event.getCore().getContributions()),
                    event.getCore().getComponentName(),
                    event.getCore().getOwner().getComponentName()),
                net.md_5.bungee.api.ChatColor.RED));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void destroyableDestroyed(final DestroyableDestroyedEvent event) {
    Destroyable destroyable = event.getDestroyable();
    if (!destroyable.isVisible()) return;

    event
        .getMatch()
        .sendMessage(
            new PersonalizedTranslatable(
                "destroyable.complete.owned",
                formatContributions(event.getDestroyable().getContributions()),
                destroyable.getComponentName(),
                destroyable.getOwner().getComponentName()));
  }

  private Component formatContributions(Collection<? extends Contribution> contributions) {
    List<? extends Contribution> sorted = new ArrayList<>(contributions);
    sorted.sort(
        (o1, o2) -> {
          return Double.compare(o2.getPercentage(), o1.getPercentage()); // reverse
        });

    List<Component> contributors = new ArrayList<>();
    boolean someExcluded = false;
    for (Contribution entry : sorted) {
      if (entry.getPercentage() > 0.2) { // 20% necessary to be included
        contributors.add(
            new PersonalizedTranslatable(
                "objective.credit.percentage",
                entry.getPlayerState().getStyledName(NameStyle.COLOR),
                new PersonalizedText(
                    String.valueOf(Math.round(entry.getPercentage() * 100)),
                    net.md_5.bungee.api.ChatColor.AQUA)));
      } else {
        someExcluded = true;
      }
    }

    Component credit;
    if (contributors.isEmpty()) {
      credit =
          someExcluded
              ? new PersonalizedTranslatable("objective.credit.many") // All contributors < 20%
              : new PersonalizedTranslatable("objective.credit.unknown"); // No contributors
    } else {
      if (someExcluded) {
        contributors.add(
            new PersonalizedTranslatable("objective.credit.etc")); // Some contributors < 20%
      }
      credit = TranslationUtils.combineComponents(contributors);
    }

    return credit;
  }
}

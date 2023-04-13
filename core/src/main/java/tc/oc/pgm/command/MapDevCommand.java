package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.variables.Variable;

public class MapDevCommand {

  @CommandMethod("variables [target] [page]")
  @CommandDescription("Inspect variables for a player")
  @CommandPermission(Permissions.DEBUG)
  public void showVariables(
      Audience audience,
      CommandSender sender,
      Match match,
      @Argument(value = "target", defaultValue = CURRENT) MatchPlayer target,
      @Argument(value = "page", defaultValue = "1") int page,
      @Flag(value = "query", aliases = "q") String query,
      @Flag(value = "all", aliases = "a") boolean all) {

    List<Variable<?>> variables =
        match.getFeatureContext().getAll().stream()
            .filter(Variable.class::isInstance)
            .map(v -> (Variable<?>) v)
            .filter(v -> query == null || v.getId().contains(query))
            .sorted(Comparator.comparing(Variable::getId))
            .collect(Collectors.toList());

    int resultsPerPage = all ? variables.size() : 8;
    int pages = all ? 1 : (variables.size() + resultsPerPage - 1) / resultsPerPage;

    Component title =
        TextFormatter.paginate(
            text("Variables for ").append(target.getName()),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            true);
    Component header = TextFormatter.horizontalLineHeading(sender, title, NamedTextColor.BLUE);

    PrettyPaginatedComponentResults.display(
        audience,
        variables,
        page,
        resultsPerPage,
        header,
        (v, i) ->
            text().append(text(v.getId() + ": ", NamedTextColor.AQUA), text(v.getValue(target))));
  }

  @CommandMethod("filter <filter> [target]")
  @CommandDescription("Match a filter against a player")
  @CommandPermission(Permissions.DEBUG)
  public void evaluateFilter(
      Audience audience,
      @Argument("filter") Filter filter,
      @Argument(value = "target", defaultValue = CURRENT) MatchPlayer target) {
    audience.sendMessage(
        text("Filter responded with ", NamedTextColor.YELLOW)
            .append(text(filter.query(target) + "", NamedTextColor.AQUA))
            .append(text(" to ", NamedTextColor.YELLOW))
            .append(target.getName()));
  }
}

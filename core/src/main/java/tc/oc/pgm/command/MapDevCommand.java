package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.join;
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
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
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
import tc.oc.pgm.variables.types.IndexedVariable;

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
        (v, pageIndex) -> {
          ComponentLike value;
          if (v.isIndexed()) {
            IndexedVariable<?> idx = (IndexedVariable<?>) v;
            List<Component> values =
                IntStream.range(0, idx.size())
                    .mapToObj(i -> text(idx.getValue(target, i)))
                    .collect(Collectors.toList());
            value = join(JoinConfiguration.commas(true), values);
          } else {
            value = text(v.getValue(target));
          }

          return text().append(text(v.getId() + ": ", NamedTextColor.AQUA), value);
        });
  }

  @CommandMethod("variable set <variable> <value> [target]")
  @CommandDescription("Inspect variables for a player")
  @CommandPermission(Permissions.DEBUG)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void setVariable(
      Audience audience,
      @Argument("variable") Variable variable,
      @Argument("value") double value,
      @Argument(value = "target", defaultValue = CURRENT) MatchPlayer target) {
    variable.setValue(target, value);
    audience.sendMessage(
        text("Variable ", NamedTextColor.YELLOW)
            .append(text(variable.getId(), NamedTextColor.AQUA))
            .append(text(" set to ", NamedTextColor.YELLOW))
            .append(text(value + "", NamedTextColor.AQUA))
            .append(text(" for ", NamedTextColor.YELLOW))
            .append(target.getName()));
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

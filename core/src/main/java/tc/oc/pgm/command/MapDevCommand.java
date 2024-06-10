package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
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

  // Avoid showing too many values. Messages that are too long kick the client.
  private static final int ARRAY_CAP = 16;

  @Command("variables [target] [page]")
  @CommandDescription("Inspect variables for a player")
  @Permission(Permissions.DEBUG)
  public void showVariables(
      Audience audience,
      CommandSender sender,
      Match match,
      @Argument("target") @Default(CURRENT) MatchPlayer target,
      @Argument("page") @Default("1") int page,
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
          Component value;
          if (v.isIndexed()) {
            IndexedVariable<?> idx = (IndexedVariable<?>) v;
            value =
                join(
                    JoinConfiguration.commas(true),
                    IntStream.range(0, Math.min(ARRAY_CAP, idx.size()))
                        .mapToObj(i -> text(idx.getValue(target, i)))
                        .collect(Collectors.toList()));
            if (idx.size() > ARRAY_CAP) value = value.append(text(" ..."));
          } else {
            value = text(v.getValue(target));
          }

          return text().append(text(v.getId() + ": ", NamedTextColor.AQUA), value);
        });
  }

  @Command("variable set <variable> <value> [target]")
  @CommandDescription("Inspect variables for a player")
  @Permission(Permissions.DEBUG)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void setVariable(
      Audience audience,
      @Argument("variable") Variable variable,
      @Argument("value") double value,
      @Argument("target") @Default(CURRENT) MatchPlayer target) {
    variable.setValue(target, value);
    audience.sendMessage(
        text("Variable ", NamedTextColor.YELLOW)
            .append(text(variable.getId(), NamedTextColor.AQUA))
            .append(text(" set to ", NamedTextColor.YELLOW))
            .append(text(value + "", NamedTextColor.AQUA))
            .append(text(" for ", NamedTextColor.YELLOW))
            .append(target.getName()));
  }

  @Command("filter <filter> [target]")
  @CommandDescription("Match a filter against a player")
  @Permission(Permissions.DEBUG)
  public void evaluateFilter(
      Audience audience,
      @Argument("filter") Filter filter,
      @Argument("target") @Default(CURRENT) MatchPlayer target) {
    audience.sendMessage(
        text("Filter responded with ", NamedTextColor.YELLOW)
            .append(text(filter.query(target) + "", NamedTextColor.AQUA))
            .append(text(" to ", NamedTextColor.YELLOW))
            .append(target.getName()));
  }
}

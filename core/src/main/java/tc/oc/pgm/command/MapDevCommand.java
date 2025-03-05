package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;
import static tc.oc.pgm.util.text.TextException.exception;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

public class MapDevCommand {

  // Avoid showing too many values. Messages that are too long kick the client.
  private static final int ARRAY_CAP = 16;

  @Command("variables [target] [page]")
  @CommandDescription("Inspect variables for a player")
  @Permission(Permissions.DEBUG)
  public void showVariables(
      Audience audience,
      CommandSender sender,
      VariablesMatchModule vmm,
      @Argument("target") @Default(CURRENT) MatchPlayer target,
      @Argument("page") @Default("1") int page,
      @Flag(value = "query", aliases = "q") String query,
      @Flag(value = "all", aliases = "a") boolean all) {

    List<Map.Entry<String, Variable<?>>> variables = vmm.getVariables()
        .filter(e -> query == null || e.getKey().contains(query))
        .sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toList());

    int resultsPerPage = all ? variables.size() : 8;
    int pages = all ? 1 : (variables.size() + resultsPerPage - 1) / resultsPerPage;

    Component title = TextFormatter.paginate(
        text("Variables for ").append(target.getName()),
        page,
        pages,
        NamedTextColor.DARK_AQUA,
        NamedTextColor.AQUA,
        true);
    Component header = TextFormatter.horizontalLineHeading(sender, title, NamedTextColor.BLUE);

    PrettyPaginatedComponentResults.display(
        audience, variables, page, resultsPerPage, header, (e, pageIndex) -> {
          Component value;
          if (e.getValue().isIndexed() && e.getValue() instanceof Variable.Indexed<?> idx) {
            value = join(
                JoinConfiguration.commas(true),
                IntStream.range(0, Math.min(ARRAY_CAP, idx.size()))
                    .mapToObj(i -> text(idx.getValue(target, i)))
                    .collect(Collectors.toList()));
            if (idx.size() > ARRAY_CAP) value = value.append(text(" ..."));
          } else {
            value = text(e.getValue().getValue(target));
          }

          return text().append(text(e.getKey() + ": ", NamedTextColor.AQUA), value);
        });
  }

  @Command("variable set <variable> <value> [target]")
  @CommandDescription("Inspect variables for a player")
  @Permission(Permissions.DEBUG)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void setVariable(
      VariablesMatchModule vmm,
      Audience audience,
      @Argument("variable") Variable variable,
      @Argument("value") double value,
      @Argument("target") @Default(CURRENT) MatchPlayer target) {
    variable.setValue(target, value);
    audience.sendMessage(text("Variable ", NamedTextColor.YELLOW)
        .append(text(vmm.getId(variable), NamedTextColor.AQUA))
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
    audience.sendMessage(text("Filter responded with ", NamedTextColor.YELLOW)
        .append(text(filter.query(target) + "", NamedTextColor.AQUA))
        .append(text(" to ", NamedTextColor.YELLOW))
        .append(target.getName()));
  }

  @Command("region <region>")
  @CommandDescription("Visualize a region")
  @Permission(Permissions.DEBUG)
  public void visualizeRegion(MatchPlayer viewer, @Argument("region") Region region) {
    var pl = viewer.getBukkit();
    var world = pl.getWorld();
    var reg = region.getStatic(world);
    if (!reg.getBounds().isBlockFinite()) {
      throw exception("Region is not finite");
    }
    new DisplayRunner(pl, reg);
  }

  private static class DisplayRunner implements Runnable {
    private static final BlockMaterialData GLASS = MaterialData.block(Material.GLASS);
    private static final float SPREAD_MUL = 0.2f;
    private static final float COUNT_MUL = 0.1f;

    private final Player player;
    private final Region.Static region;
    private final Location loc;

    private final float[][] directions;
    private final double[][] edges, vertices;

    private final Future<?> task;
    private int ticks = 150;

    private DisplayRunner(Player player, Region.Static region) {
      this.player = player;
      this.region = region;
      this.loc = new Location(player.getWorld(), 0, 0, 0);

      var bounds = region.getBounds();
      Vector min = bounds.getMin(), max = bounds.getMax();
      boolean hasBottom = min.getY() >= -64, hasTop = max.getY() <= 320;
      if (!hasBottom) min.setY(-32);
      if (!hasTop) max.setY(288);

      var center = min.clone().add(max).multiply(0.5);
      var size = max.clone().subtract(min);

      this.directions = new float[][] {
        {(float) size.getX() * SPREAD_MUL, 0, 0, (float) Math.max(10, size.getX() * COUNT_MUL)},
        {0, (float) size.getY() * SPREAD_MUL, 0, (float) Math.max(10, size.getY() * COUNT_MUL)},
        {0, 0, (float) size.getZ() * SPREAD_MUL, (float) Math.max(10, size.getZ() * COUNT_MUL)}
      };
      if (!hasBottom) min.setY(Double.NaN);
      if (!hasTop) max.setY(Double.NaN);

      this.edges = new double[][] {
        {center.getX(), min.getY(), min.getZ()},
        {center.getX(), min.getY(), max.getZ()},
        {center.getX(), max.getY(), min.getZ()},
        {center.getX(), max.getY(), max.getZ()},
        {min.getX(), center.getY(), min.getZ()},
        {min.getX(), center.getY(), max.getZ()},
        {max.getX(), center.getY(), min.getZ()},
        {max.getX(), center.getY(), max.getZ()},
        {min.getX(), min.getY(), center.getZ()},
        {min.getX(), max.getY(), center.getZ()},
        {max.getX(), min.getY(), center.getZ()},
        {max.getX(), max.getY(), center.getZ()},
      };
      this.vertices = new double[][] {
        {min.getX(), min.getY(), min.getZ()},
        {min.getX(), min.getY(), max.getZ()},
        {min.getX(), max.getY(), min.getZ()},
        {min.getX(), max.getY(), max.getZ()},
        {max.getX(), min.getY(), min.getZ()},
        {max.getX(), min.getY(), max.getZ()},
        {max.getX(), max.getY(), min.getZ()},
        {max.getX(), max.getY(), max.getZ()},
      };

      region.getBlockVectors().forEach(bv -> {
        var b = BlockVectors.blockAt(loc.getWorld(), bv);
        for (var dir : BlockFaces.NEIGHBORS) {
          if (!region.contains(b.getRelative(dir))) {
            GLASS.sendBlockChange(player, b.getLocation());
            break;
          }
        }
      });

      task = PGM.get().getExecutor().scheduleWithFixedDelay(this, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
      if (ticks-- < 0) {
        task.cancel(true);
        region.getBlocks(player.getWorld()).forEach(b -> MaterialData.block(b)
            .sendBlockChange(player, b.getLocation()));
        return;
      }
      for (int i = 0; i < edges.length; i++) {
        if (Double.isNaN(edges[i][1])) continue;
        loc.set(edges[i][0], edges[i][1], edges[i][2]);
        var dirs = directions[(i / 4)];
        EFFECTS.spawnFlame(player, loc, dirs[0], dirs[1], dirs[2], (int) dirs[3]);
      }
      for (double[] vertex : vertices) {
        if (Double.isNaN(vertex[1])) continue;
        loc.set(vertex[0], vertex[1], vertex[2]);
        EFFECTS.coloredDust(player, loc, Color.RED);
      }
    }
  }
}

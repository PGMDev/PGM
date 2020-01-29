package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.destroyable.DestroyableMatchModule;

public class DestroyableCommands {
  private static DestroyableMatchModule matchModule(CommandSender sender) throws CommandException {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    DestroyableMatchModule dmm = match.getModule(DestroyableMatchModule.class);
    if (dmm == null) throw new CommandException("No destroyables");
    return dmm;
  }

  private static Destroyable lookup(CommandSender sender, String id) throws CommandException {
    Match match = PGM.get().getMatchManager().getMatch(sender);

    Destroyable destroyable = match.getFeatureContext().get(id, Destroyable.class);
    if (destroyable != null) return destroyable;

    for (Destroyable feature : match.getFeatureContext().getAll(Destroyable.class)) {
      if (feature.getId().startsWith(id)) {
        if (destroyable != null)
          throw new CommandException("Multiple destroyables have IDs that start with '" + id + "'");
        destroyable = feature;
      }
    }
    if (destroyable != null) return destroyable;

    throw new CommandException("No destroyable with ID '" + id + "'");
  }

  private static void assertEditPerms(CommandSender sender) throws CommandException {
    if (!sender.hasPermission(Permissions.GAMEPLAY)) {
      throw new CommandException("You don't have permission to do that");
    }
  }

  @Command(
      aliases = {"destroyable", "destroyables"},
      desc = "Commands for working with destroyables (monuments)",
      usage = "[id [completion[=(<blocks>|<percent>%)]]]")
  public static void destroyable(
      CommandSender sender, Optional<String> optionalID, Optional<String> optionalAction)
      throws CommandException {

    if (!optionalID.isPresent()) {
      list(sender);
    } else {
      String id = optionalID.get();
      Destroyable destroyable = lookup(sender, id);

      if (!optionalAction.isPresent()) {
        details(sender, destroyable);
        return;
      }

      String action = optionalAction.get();

      if (action.startsWith("completion")) {
        if (action.startsWith("completion=")) {
          setCompletion(sender, destroyable, action.substring("completion=".length()));
        }
        completion(sender, destroyable);
      } else {
        throw new CommandException("Invalid action '" + action + "'");
      }
    }
  }

  public static void list(CommandSender sender) throws CommandException {
    List<String> lines = new ArrayList<>();
    lines.add(ChatColor.GRAY + "Destroyables");

    for (Destroyable destroyable : matchModule(sender).getDestroyables()) {
      lines.add(
          "  "
              + ChatColor.AQUA
              + destroyable.getId()
              + ": "
              + ChatColor.WHITE
              + destroyable.getName()
              + ChatColor.GRAY
              + " owned by "
              + destroyable.getOwner().getColoredName());
    }

    sender.sendMessage(lines.toArray(new String[lines.size()]));
  }

  private static String formatProperty(String name, Object value) {
    return "  " + ChatColor.GRAY + name + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + value;
  }

  public static void details(CommandSender sender, Destroyable destroyable)
      throws CommandException {
    List<String> lines = new ArrayList<>();

    lines.add(ChatColor.GRAY + "Destroyable " + ChatColor.AQUA + destroyable.getId());
    lines.add(formatProperty("name", destroyable.getName()));
    lines.add(formatProperty("owner", destroyable.getOwner().getColoredName()));
    lines.add(formatProperty("size", destroyable.getMaxHealth()));
    lines.add(
        formatProperty(
            "completion",
            destroyable.getBreaksRequired()
                + " ("
                + destroyable.renderDestructionRequired()
                + ")"));

    sender.sendMessage(lines.toArray(new String[lines.size()]));
  }

  public static void completion(CommandSender sender, Destroyable destroyable)
      throws CommandException {
    sender.sendMessage(
        ChatColor.GRAY
            + "Destroyable "
            + ChatColor.AQUA
            + destroyable.getId()
            + ChatColor.GRAY
            + " has completion "
            + ChatColor.WHITE
            + Math.round(destroyable.getDestructionRequired() * 100)
            + "%"
            + ChatColor.GRAY
            + " or "
            + ChatColor.WHITE
            + destroyable.getBreaksRequired()
            + "/"
            + destroyable.getMaxHealth()
            + ChatColor.GRAY
            + " breaks");
  }

  public static void setCompletion(CommandSender sender, Destroyable destroyable, String value)
      throws CommandException {
    assertEditPerms(sender);

    Double completion;
    Integer breaks;

    if (value.endsWith("%")) {
      completion = Double.parseDouble(value.substring(0, value.length() - 1)) / 100d;
      breaks = null;
    } else {
      completion = null;
      breaks = Integer.parseInt(value);
    }

    try {
      if (completion != null) {
        destroyable.setDestructionRequired(completion);
      } else if (breaks != null) {
        destroyable.setBreaksRequired(breaks);
      }
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    }
  }
}

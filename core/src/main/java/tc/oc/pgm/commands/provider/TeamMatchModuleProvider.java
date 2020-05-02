package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextTranslations;

public class TeamMatchModuleProvider implements BukkitProvider<TeamMatchModule> {

  private final MatchManager matchManager;

  public TeamMatchModuleProvider(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  @Override
  public boolean isProvided() {
    return true;
  }

  @Nullable
  @Override
  public TeamMatchModule get(
      CommandSender sender, CommandArgs commandArgs, List<? extends Annotation> list)
      throws ProvisionException {
    TeamMatchModule teamMatchModule =
        matchManager.getMatch(sender).getModule(TeamMatchModule.class);
    if (teamMatchModule == null) {
      throw new ProvisionException(TextTranslations.translate("command.noTeams", sender));
    }
    return teamMatchModule;
  }
}

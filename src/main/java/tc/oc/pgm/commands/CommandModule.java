package tc.oc.pgm.commands;

import app.ashcon.intake.parametric.AbstractModule;
import org.bukkit.util.Vector;
import org.joda.time.Duration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.commands.provider.*;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.teams.TeamMatchModule;

public class CommandModule extends AbstractModule {

  private final PGM pgm;

  public CommandModule(PGM pgm) {
    this.pgm = pgm;
  }

  @Override
  protected void configure() {
    configureInstances();
    configureProviders();
  }

  private void configureInstances() {
    bind(PGM.class).toInstance(pgm);
    bind(MatchManager.class).toInstance(pgm.getMatchManager());
    bind(MapLibrary.class).toInstance(pgm.getMapLibrary());
  }

  private void configureProviders() {
    bind(Audience.class).toProvider(new AudienceProvider());
    bind(Match.class).toProvider(new MatchProvider(pgm.getMatchManager()));
    bind(MatchPlayer.class).toProvider(new MatchPlayerProvider(pgm.getMatchManager()));
    bind(PGMMap.class).toProvider(new PGMMapProvider(pgm.getMatchManager(), pgm.getMapLibrary()));
    bind(Duration.class).toProvider(new DurationProvider());
    bind(TeamMatchModule.class).toProvider(new TeamMatchModuleProvider(pgm.getMatchManager()));
    bind(Vector.class).toProvider(new VectorProvider());
  }
}

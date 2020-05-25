package tc.oc.pgm.community.command;

import tc.oc.pgm.command.graph.CommandGraph;

public class CommunityCommandGraph extends CommandGraph {

  @Override
  public void registerAll() {
    super.registerAll();

    register(new ReportCommand());
    register(new ModerationCommand());
  }
}

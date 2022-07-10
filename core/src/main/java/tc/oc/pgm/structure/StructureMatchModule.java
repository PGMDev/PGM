package tc.oc.pgm.structure;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;

public class StructureMatchModule implements MatchModule {

  private final DynamicScheduler scheduler;
  private final Match match;
  private final List<DynamicDefinition> dynamicDefinitions;

  public StructureMatchModule(Match match, List<DynamicDefinition> dynamicDefinitions) {
    this.match = match;
    this.dynamicDefinitions = dynamicDefinitions;
    this.scheduler =
        new DynamicScheduler(
            match,
            Comparator.comparing(
                dynamic ->
                    this.dynamicDefinitions.indexOf(
                        dynamic.getDefinition()))); // TODO does this sort backwards?

  }

  @Override
  public void load() throws ModuleLoadException {
    List<Dynamic> dynamics =
        dynamicDefinitions.stream().map(d -> new Dynamic(d, match)).collect(Collectors.toList());

    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    for (Dynamic dynamic : dynamics) {
      match.getFeatureContext().add(dynamic);
      final DynamicDefinition dynamicDef = dynamic.getDefinition();
      fmm.onChange(
          MatchPlayer.class, // TODO make this scope adjustable
          dynamicDef.getTrigger(),
          (m, response) -> {
            if (response) {
              // This is the passive filter, not the dynamic one
              if (dynamicDef.getPassive().query(m).isAllowed()) {
                this.scheduler.queuePlace(dynamic);
              }
            } else {
              this.scheduler.queueClear(dynamic);
            }
          });
    }
  }
}

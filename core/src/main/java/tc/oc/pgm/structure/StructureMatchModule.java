package tc.oc.pgm.structure;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;

public class StructureMatchModule implements MatchModule {

  private final Match match;
  private final List<DynamicStructureDefinition> dynamicStructureDefinitions;
  private final Queue<DynamicStructure> clearQueue;
  private final Queue<DynamicStructure> placeQueue;

  public StructureMatchModule(
      Match match, List<DynamicStructureDefinition> dynamicStructureDefinitions) {
    this.match = match;
    this.dynamicStructureDefinitions = dynamicStructureDefinitions;
    Comparator<DynamicStructure> order =
        Comparator.comparing(
            dynamicStructure ->
                this.dynamicStructureDefinitions.indexOf(dynamicStructure.getDefinition()));

    this.clearQueue = new PriorityQueue<>(order);
    this.placeQueue = new PriorityQueue<>(order);
  }

  @Override
  public void load() throws ModuleLoadException {
    List<DynamicStructure> dynamicStructures =
        dynamicStructureDefinitions.stream()
            .map(d -> new DynamicStructure(d, match))
            .collect(Collectors.toList());

    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    for (DynamicStructure dynamicStructure : dynamicStructures) {
      match.getFeatureContext().add(dynamicStructure);
      final DynamicStructureDefinition dynamicDef = dynamicStructure.getDefinition();
      fmm.onChange(
          MatchPlayer.class, // TODO make this scope adjustable
          dynamicDef.getTrigger(),
          (m, response) -> {
            if (response) {
              // This is the passive filter, not the dynamic one
              if (dynamicDef.getPassive().query(m).isAllowed()) {
                this.queuePlace(dynamicStructure);
              }
            } else {
              this.queueClear(dynamicStructure);
            }
          });
    }
  }

  private void queuePlace(DynamicStructure dynamicStructure) {
    clearQueue.remove(dynamicStructure);
    placeQueue.add(dynamicStructure);
    schedule();
  }

  private void queueClear(DynamicStructure dynamicStructure) {
    placeQueue.remove(dynamicStructure);
    clearQueue.add(dynamicStructure);
    schedule();
  }

  private void schedule() {
    match.getExecutor(MatchScope.LOADED).submit(this::process);
  }

  /**
   * We need to make sure that dynamics are placed and cleared in definition order, and that clears
   * happen before placements.
   */
  private void process() {
    while (!clearQueue.isEmpty()) {
      clearQueue.poll().clear();
    }

    while (!placeQueue.isEmpty()) {
      placeQueue.poll().place();
    }
  }
}

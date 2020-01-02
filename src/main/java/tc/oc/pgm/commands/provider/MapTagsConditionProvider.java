package tc.oc.pgm.commands.provider;

import static com.google.common.base.Preconditions.*;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.Namespace;
import app.ashcon.intake.parametric.Provider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.maptag.MapTagSet;
import tc.oc.pgm.maptag.MapTagsCondition;

public class MapTagsConditionProvider implements Provider<MapTagsCondition> {

  private static final String PREFIX = Character.toString(MapTag.SYMBOL);
  private static final String NEGATION = "!";

  private final MapLibrary mapLibrary;

  public MapTagsConditionProvider(MapLibrary mapLibrary) {
    this.mapLibrary = checkNotNull(mapLibrary);
  }

  @Override
  public String getName() {
    return "maptags";
  }

  @Nullable
  @Override
  public MapTagsCondition get(CommandArgs args, List<? extends Annotation> list)
      throws ArgumentException, ProvisionException {
    Map<MapTag, Boolean> mapTags = new HashMap<>();
    while (args.hasNext()) {
      String query = args.next().toLowerCase(Locale.ROOT);
      boolean allow = true;

      if (query.startsWith(NEGATION)) {
        query = query.substring(NEGATION.length());
        allow = false;
      }

      if (!query.startsWith(PREFIX)) {
        continue;
      }
      query = query.substring(PREFIX.length());

      if (!query.isEmpty()) {
        mapTags.put(MapTag.forName(query), allow);
      }
    }

    return new MapTagsCondition(mapTags);
  }

  @Override
  public List<String> getSuggestions(
      String prefix, Namespace namespace, List<? extends Annotation> modifiers) {
    String query = prefix.toLowerCase(Locale.ROOT);
    if (!query.startsWith(PREFIX)) {
      return Collections.emptyList();
    }
    query = query.substring(PREFIX.length());

    MapTagSet mapTags = MapTagSet.mutable();
    for (PGMMap map : mapLibrary.getMaps()) {
      for (MapTag mapTag : map.getPersistentContext().getMapTags()) {
        if (mapTag.getName().startsWith(query)) {
          mapTags.add(mapTag);
        }
      }
    }

    return mapTags.stream().map(MapTag::toString).collect(Collectors.toList());
  }
}

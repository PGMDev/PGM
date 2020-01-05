package tc.oc.pgm.commands.provider;

import static com.google.common.base.Preconditions.*;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.Namespace;
import app.ashcon.intake.parametric.Provider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.maptag.MapTagsCondition;
import tc.oc.util.Pair;

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
      parseMapTag(args.next())
          .ifPresent(
              pair -> {
                if (!pair.first.isEmpty()) {
                  mapTags.put(MapTag.forName(pair.first), pair.second);
                }
              });
    }

    return new MapTagsCondition(mapTags);
  }

  @Override
  public List<String> getSuggestions(
      String prefix, Namespace namespace, List<? extends Annotation> modifiers) {
    return parseMapTag(prefix)
        .map(
            pair -> {
              Set<String> mapTags = new TreeSet<>(Comparator.naturalOrder());
              for (PGMMap map : mapLibrary.getMaps()) {
                for (MapTag mapTag : map.getPersistentContext().getMapTags()) {
                  if (mapTag.getName().startsWith(pair.first)) {
                    mapTags.add(mapTagToString(mapTag, pair.second));
                  }
                }
              }

              return (List<String>) new ArrayList<>(mapTags);
            })
        .orElse(Collections.emptyList());
  }

  private Optional<Pair<String, Boolean>> parseMapTag(String query) {
    checkNotNull(query);
    query = query.toLowerCase(Locale.ROOT);
    boolean allow = true;

    if (query.startsWith(NEGATION)) {
      query = query.substring(NEGATION.length());
      allow = false;
    }

    if (!query.startsWith(PREFIX)) {
      return Optional.empty();
    }

    query = query.substring(PREFIX.length());
    return Optional.of(Pair.create(query, allow));
  }

  private String mapTagToString(MapTag mapTag, boolean allow) {
    StringBuilder toString = new StringBuilder();
    if (!allow) {
      toString.append(NEGATION);
    }
    toString.append(mapTag.toString());
    return toString.toString();
  }
}

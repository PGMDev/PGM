package tc.oc.pgm.namedecorations;

import java.util.UUID;

/** This interface is intended to return what prefix and suffix a player should have */
public interface NameDecorationProvider {

  String getPrefix(UUID uuid);

  String getSuffix(UUID uuid);
}

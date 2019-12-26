package tc.oc.pgm.prefix;

import java.util.UUID;

public interface PrefixProvider {

  void setPrefix(UUID uuid, String string);

  String getPrefix(UUID uuid);

  void removePlayer(UUID uuid);
}

package tc.oc.pgm.util.tablist;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

/** Implements part of {@link TabEntry} with a few generally useful properties */
public abstract class SimpleTabEntry implements TabEntry {
  // Dark grey square
  private static final Skin DEFAULT_SKIN =
      new Skin(
          "eyJ0aW1lc3RhbXAiOjE0MTEyNjg3OTI3NjUsInByb2ZpbGVJZCI6IjNmYmVjN2RkMGE1ZjQwYmY5ZDExODg1YTU0NTA3MTEyIiwicHJvZmlsZU5hbWUiOiJsYXN0X3VzZXJuYW1lIiwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzg0N2I1Mjc5OTg0NjUxNTRhZDZjMjM4YTFlM2MyZGQzZTMyOTY1MzUyZTNhNjRmMzZlMTZhOTQwNWFiOCJ9fX0=",
          "u8sG8tlbmiekrfAdQjy4nXIcCfNdnUZzXSx9BE1X5K27NiUvE1dDNIeBBSPdZzQG1kHGijuokuHPdNi/KXHZkQM7OJ4aCu5JiUoOY28uz3wZhW4D+KG3dH4ei5ww2KwvjcqVL7LFKfr/ONU5Hvi7MIIty1eKpoGDYpWj3WjnbN4ye5Zo88I2ZEkP1wBw2eDDN4P3YEDYTumQndcbXFPuRRTntoGdZq3N5EBKfDZxlw4L3pgkcSLU5rWkd5UH4ZUOHAP/VaJ04mpFLsFXzzdU4xNZ5fthCwxwVBNLtHRWO26k/qcVBzvEXtKGFJmxfLGCzXScET/OjUBak/JEkkRG2m+kpmBMgFRNtjyZgQ1w08U6HHnLTiAiio3JswPlW5v56pGWRHQT5XWSkfnrXDalxtSmPnB5LmacpIImKgL8V9wLnWvBzI7SHjlyQbbgd+kUOkLlu7+717ySDEJwsFJekfuR6N/rpcYgNZYrxDwe4w57uDPlwNL6cJPfNUHV7WEbIU1pMgxsxaXe8WSvV87qLsR7H06xocl2C0JFfe2jZR4Zh3k9xzEnfCeFKBgGb4lrOWBu1eDWYgtKV67M2Y+B3W5pjuAjwAxn0waODtEn/3jKPbc/sxbPvljUCw65X+ok0UUN1eOwXV5l2EGzn05t3Yhwq19/GxARg63ISGE8CKw=");

  protected static UUID randomUUIDVersion2() {
    UUID uuid = UUID.randomUUID();
    return new UUID(
        (uuid.getMostSignificantBits() & ~0xf000) | 0x2000, uuid.getLeastSignificantBits());
  }

  private final UUID uuid;
  private final String name;

  protected SimpleTabEntry(UUID uuid) {
    this.uuid = uuid;

    String name = this.uuid.toString();
    // Use a | and the last 15 chars, most likely to be unique, and appear sorted at the end
    this.name = "|" + name.substring(name.length() - 15);
  }

  protected SimpleTabEntry() {
    this(randomUUIDVersion2());
  }

  @Override
  public UUID getId() {
    return this.uuid;
  }

  @Override
  public String getName(TabView view) {
    return this.name;
  }

  @Override
  public GameMode getGamemode() {
    return GameMode.CREATIVE;
  }

  @Override
  public int getPing() {
    return 9999;
  }

  @Override
  public @Nullable Skin getSkin(TabView view) {
    return DEFAULT_SKIN;
  }

  @Override
  public @Nullable Player getFakePlayer(TabView view) {
    return null;
  }

  @Override
  public int getFakeEntityId(TabView view) {
    return -1;
  }
}

package tc.oc.pgm.map;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.api.map.MapSource.DEFAULT_VARIANT;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.world.WorldStorage.WORLD_STORAGE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Difficulty;
import org.jdom2.Element;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.api.map.VariantInfo;
import tc.oc.pgm.api.map.WorldInfo;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.map.contrib.PlayerContributor;
import tc.oc.pgm.map.contrib.PseudonymContributor;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.StreamUtils;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.world.WorldFormat;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapInfoImpl implements MapInfo {
  // See #parseWorld, this class actually is responsible for terrain tag.
  private static final MapTag TERRAIN = new MapTag("terrain", "Terrain");

  private final MapSource source;

  private final VariantInfo variant;
  private final Map<String, VariantInfo> variants;

  private final Version proto;
  private final Version version;
  private final Phase phase;
  private final String normalizedName;
  private final String description;
  private final LocalDate created;
  private final Collection<Contributor> authors;
  private final Collection<Contributor> contributors;
  private final Collection<String> rules;
  private final Component gamemode;
  private final int difficulty;
  private final WorldInfo world;
  private final boolean friendlyFire;

  // May be set after loading the whole context
  protected Collection<Gamemode> gamemodes;

  // Must be set after loading the whole context
  protected Collection<MapTag> tags = ImmutableSortedSet.of();
  protected Collection<Integer> players = ImmutableList.of();
  protected SoftReference<MapContext> context = null;

  public MapInfoImpl(MapSource source, @Nullable Map<String, VariantInfo> variants, Element root)
      throws InvalidXMLException {
    this.source = source;
    this.variants = variants == null ? createVariantMap(root) : variants;
    this.variant = this.variants.get(source.getVariantId());

    if (variant == null) throw new InvalidXMLException("Could not find variant definition", root);

    this.normalizedName = StringUtils.normalize(variant.getName());

    this.proto = assertNotNull(XMLUtils.parseSemanticVersion(Node.fromRequiredAttr(root, "proto")));
    this.version =
        assertNotNull(XMLUtils.parseSemanticVersion(Node.fromRequiredChildOrAttr(root, "version")));
    this.description = assertNotNull(
        Node.fromRequiredChildOrAttr(root, "objective", "description").getValueNormalize());
    this.created = XMLUtils.parseDate(Node.fromChildOrAttr(root, "created"));
    this.authors = parseContributors(root, "author");
    this.contributors = parseContributors(root, "contributor");
    this.rules = parseRules(root);
    this.difficulty = XMLUtils.parseEnum(
            Node.fromLastChildOrAttr(root, "difficulty"), Difficulty.class, Difficulty.NORMAL)
        .ordinal();
    this.world = parseWorld(root);
    this.gamemode = parseGamemode(root);
    this.gamemodes = parseGamemodes(root);
    this.phase =
        XMLUtils.parseEnum(Node.fromLastChildOrAttr(root, "phase"), Phase.class, Phase.PRODUCTION);
    this.friendlyFire = XMLUtils.parseBoolean(
        Node.fromLastChildOrAttr(root, "friendlyfire", "friendly-fire"), false);
  }

  @NonNull
  private Map<String, VariantInfo> createVariantMap(Element root) throws InvalidXMLException {
    // Most variants share a world folder, so only inspect each one once
    Map<Path, WorldData> worlds = new HashMap<>();

    LinkedHashMap<String, VariantInfo> variants = new LinkedHashMap<>();
    for (Element el : root.getChildren("variant")) {
      VariantData vd = new VariantData(root, el, worlds);
      if (variants.put(vd.variantId, vd) != null)
        throw new InvalidXMLException("Duplicate variant ids are not allowed", el);
    }
    if (!variants.containsKey(DEFAULT_VARIANT))
      variants.putFirst(DEFAULT_VARIANT, new VariantData(root, null, worlds));
    return ImmutableMap.copyOf(variants);
  }

  /** The state of a world folder, as read while parsing the map */
  private record WorldData(WorldFormat format, int dataVersion) {
    static WorldData read(Path worldDir) {
      var format = WorldFormat.detect(worldDir);
      return new WorldData(format, WORLD_STORAGE.getWorldDataVersion(worldDir, format));
    }
  }

  @Override
  public String getBaseId() {
    return variants.get(DEFAULT_VARIANT).getId();
  }

  @Override
  public VariantInfo getVariant() {
    return variant;
  }

  @Override
  public Map<String, VariantInfo> getVariants() {
    return variants;
  }

  @Override
  public Version getProto() {
    return proto;
  }

  @Override
  public Version getVersion() {
    return version;
  }

  @Override
  public String getNormalizedName() {
    return normalizedName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public LocalDate getCreated() {
    return created;
  }

  @Override
  public Collection<Contributor> getAuthors() {
    return authors;
  }

  @Override
  public Collection<Contributor> getContributors() {
    return contributors;
  }

  @Override
  public Collection<String> getRules() {
    return rules;
  }

  @Override
  public int getDifficulty() {
    return difficulty;
  }

  @Override
  public Collection<MapTag> getTags() {
    return Collections.unmodifiableCollection(tags);
  }

  @Override
  public Collection<Integer> getMaxPlayers() {
    return players;
  }

  @Override
  public Component getGamemode() {
    return gamemode;
  }

  @Override
  public Collection<Gamemode> getGamemodes() {
    return gamemodes;
  }

  @Override
  public WorldInfo getWorld() {
    return world;
  }

  @Override
  public Phase getPhase() {
    return phase;
  }

  @Override
  public boolean getFriendlyFire() {
    return friendlyFire;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapInfo)) return false;
    return getId().equals(((MapInfo) obj).getId());
  }

  @Override
  public String toString() {
    return "MapInfo{id=" + getId() + ", version=" + this.version + "}";
  }

  @Override
  public MapSource getSource() {
    return source;
  }

  @Override
  public @Nullable MapContext getContext() {
    return context != null ? context.get() : null;
  }

  @Override
  public Component getStyledName(MapNameStyle style) {
    TextComponent.Builder name = text().content(getName());

    if (style.isColor) name.color(NamedTextColor.GOLD);
    if (style.isHighlight) name.decoration(TextDecoration.UNDERLINED, true);

    Component component = name.build();

    if (style.showAuthors) {
      component = translatable(
          "misc.authorship",
          NamedTextColor.DARK_PURPLE,
          component,
          TextFormatter.list(
              getAuthors().stream()
                  .map(c -> c.getName(NameStyle.PLAIN).color(NamedTextColor.RED))
                  .collect(Collectors.toList()),
              NamedTextColor.DARK_PURPLE));
    }

    if (style.isInteractive) {
      component = component
          .hoverEvent(showText(translatable(
              "command.maps.hover", NamedTextColor.GRAY, text(getName(), NamedTextColor.GOLD))))
          .clickEvent(runCommand("/map " + getName()));
    }

    return component;
  }

  private static @NonNull List<String> parseRules(Element root) {
    return XMLUtils.flattenElements(root, "rules", "rule").stream()
        .map(Element::getTextNormalize)
        .collect(StreamUtils.toImmutableList());
  }

  private Component parseGamemode(Element root) throws InvalidXMLException {
    Component gamemode = XMLUtils.parseFormattedText(Node.fromLastChildOrAttr(root, "game"));

    if (gamemode == null) {
      for (Element title : XMLUtils.flattenElements(root, "blitz", "title")) {
        if (this.getProto().isNoOlderThan(MapProtos.REMOVE_BLITZ_TITLE)) {
          throw new InvalidXMLException(
              "<title> inside <blitz> is no longer supported, use <map game=\"...\"> or <game>",
              title);
        }
        return XMLUtils.parseFormattedText(new Node(title));
      }
    }

    return gamemode;
  }

  private static @NonNull List<Gamemode> parseGamemodes(Element root) throws InvalidXMLException {
    ImmutableList.Builder<Gamemode> gamemodes = ImmutableList.builder();
    for (Element gamemodeEl : root.getChildren("gamemode")) {
      Gamemode gm = Gamemode.byId(gamemodeEl.getText());
      if (gm == null) throw new InvalidXMLException("Unknown gamemode", gamemodeEl);
      gamemodes.add(gm);
    }
    return gamemodes.build();
  }

  private static @NonNull List<Contributor> parseContributors(Element root, String tag)
      throws InvalidXMLException {
    List<Contributor> contributors = new ArrayList<>();
    for (Element child : XMLUtils.flattenElements(root, tag + "s", tag)) {
      String name = XMLUtils.getNormalizedNullableText(child);
      UUID uuid = XMLUtils.parseUuid(Node.fromAttr(child, "uuid"));
      String contribution = XMLUtils.getNullableAttribute(child, "contribution", "contrib");

      if (name == null && uuid == null) {
        throw new InvalidXMLException("Contributor must have either a name or UUID", child);
      }

      contributors.add(
          uuid == null
              ? new PseudonymContributor(name, contribution)
              : new PlayerContributor(uuid, contribution));
    }
    return contributors;
  }

  private static @NonNull WorldInfo parseWorld(Element root) throws InvalidXMLException {
    final Element world = root.getChild("terrain");
    return world == null ? new WorldInfoImpl() : new WorldInfoImpl(world);
  }

  protected void setContext(MapContextImpl context) {
    // The first time context is loaded, set properties which can't be
    // parsed until after modules are parsed, like team sizes or tags.
    if (this.context == null) {
      ImmutableSortedSet.Builder<MapTag> tags = ImmutableSortedSet.naturalOrder();
      ImmutableList.Builder<Integer> players = ImmutableList.builder();

      for (MapModule<?> module : context.getModules()) {
        tags.addAll(module.getTags());

        if (module instanceof TeamModule)
          players.addAll(
              Iterables.transform(((TeamModule) module).getTeams(), TeamFactory::getMaxPlayers));

        if (module instanceof FreeForAllModule)
          players.add(((FreeForAllModule) module).getOptions().maxPlayers);
      }

      if (world.hasTerrain()) tags.add(TERRAIN);

      this.tags = tags.build();
      this.players = players.build();

      // If the map defines no game-modes manually, derive them from map tags, sorted by auxiliary
      // last.
      if (this.gamemodes.isEmpty()) {
        this.gamemodes = this.tags.stream()
            .filter(MapTag::isGamemode)
            .sorted(
                Comparator.comparing(MapTag::isAuxiliary).thenComparing(Comparator.naturalOrder()))
            .map(MapTag::getGamemode)
            .collect(StreamUtils.toImmutableList());
      }
    }
    this.context = new SoftReference<>(context);
  }

  private class VariantData implements VariantInfo {
    private static final Version VERSION_1_13 = new Version(1, 13, 0);
    private static final Version VERSION_26_1 = new Version(26, 1, 0);

    private final String variantId;
    private final String mapName;
    private final String mapId;
    private final boolean customId;
    private final String world;
    private final WorldFormat worldFormat;
    private final int worldDataVersion;
    private final Range<Version> serverVersions;

    public VariantData(Element root, @Nullable Element el, Map<Path, WorldData> worlds)
        throws InvalidXMLException {
      String name = assertNotNull(Node.fromRequiredChildOrAttr(root, "name").getValueNormalize());
      String slug = assertNotNull(root).getChildTextNormalize("slug");
      Node minVer = Node.fromAttr(root, "min-server-version");
      Node maxVer = Node.fromAttr(root, "max-server-version");

      this.variantId = el == null ? DEFAULT_VARIANT : XMLUtils.parseRequiredId(el);

      // Default is not allowed to change the name, nor override, nor slug
      if (DEFAULT_VARIANT.equals(variantId)) {
        this.mapName = name;
      } else {
        boolean override = XMLUtils.parseBoolean(Node.fromAttr(el, "override"), false);
        this.mapName = (override ? "" : name + ": ") + el.getTextNormalize();

        String variantSlug = el.getAttributeValue("slug");
        if (variantSlug != null) slug = variantSlug;
        else if (override) slug = null;
        else if (slug != null) slug += "_" + variantId;
      }

      // Default however, is allowed to override world and/or server versions
      if (el == null) {
        this.world = null;
      } else {
        this.world = el.getAttributeValue("world");
        minVer = fallback(Node.fromAttr(el, "min-server-version"), minVer);
        maxVer = fallback(Node.fromAttr(el, "max-server-version"), maxVer);
      }
      this.customId = slug != null;
      this.mapId = assertNotNull(slug != null ? slug : StringUtils.slugify(mapName));

      var sourceDir = source.getAbsoluteDir();
      var worldDir = world != null ? sourceDir.resolve(world) : sourceDir;
      var worldData = worlds.computeIfAbsent(worldDir, WorldData::read);
      this.worldFormat = worldData.format();
      this.worldDataVersion = worldData.dataVersion();

      Version minVersion = XMLUtils.parseSemanticVersion(minVer);
      Version maxVersion = XMLUtils.parseSemanticVersion(maxVer);

      this.serverVersions = XMLUtils.parseClosedRange(
          fallback(minVer, maxVer), inferMinimumVersion(worldData, minVersion), maxVersion);
    }

    static <T> T fallback(T obj, T fallback) {
      return obj != null ? obj : fallback;
    }

    @Override
    public String getVariantId() {
      return variantId;
    }

    @Override
    public String getId() {
      return mapId;
    }

    @Override
    public String getName() {
      return mapName;
    }

    @Override
    public boolean hasCustomId() {
      return customId;
    }

    @Override
    public String getWorldFolder() {
      return world;
    }

    @Override
    public Range<Version> getServerVersions() {
      return serverVersions;
    }

    @Override
    public WorldFormat getWorldFormat() {
      return worldFormat;
    }

    @Override
    public int getWorldDataVersion() {
      return worldDataVersion;
    }

    @Nullable
    private Version inferMinimumVersion(WorldData worldData, @Nullable Version declared) {
      final Version required;
      if (worldData.format() == WorldFormat.DIMENSION) {
        // Assume dimension format maps are at least 26.1
        required = VERSION_26_1;
      } else if (worldData.dataVersion() >= DataVersions.V1_13) {
        // Avoid legacy servers crashing when loading chunks of post-flattening maps
        required = VERSION_1_13;
      } else {
        return declared;
      }

      // A declared version may raise the floor the world requires, but never lower it
      return declared == null || declared.isOlderThan(required) ? required : declared;
    }
  }
}

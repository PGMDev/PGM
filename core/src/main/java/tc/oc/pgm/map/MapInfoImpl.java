package tc.oc.pgm.map;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;

import java.lang.ref.SoftReference;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Difficulty;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.api.map.WorldInfo;
import tc.oc.pgm.map.contrib.PlayerContributor;
import tc.oc.pgm.map.contrib.PseudonymContributor;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapInfoImpl implements MapInfo {
  private final MapSource source;

  private final String id;
  private final String variant;
  private final Version proto;
  private final Version version;
  private final Phase phase;
  private final String name;
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

  protected final Collection<MapTag> tags;
  protected final Collection<Integer> players;
  protected final Collection<Gamemode> gamemodes;

  protected SoftReference<MapContext> context;

  public MapInfoImpl(MapSource source, Element root) throws InvalidXMLException {
    this.source = source;
    this.variant = source.getVariant();

    String tmpName = assertNotNull(Node.fromRequiredChildOrAttr(root, "name").getValueNormalize());
    if (variant != null) {
      Element variantEl =
          root.getChildren("variant").stream()
              .filter(el -> Objects.equals(variant, el.getAttributeValue("id")))
              .findFirst()
              .orElseThrow(
                  () -> new InvalidXMLException("Could not find variant definition", root));

      boolean override = XMLUtils.parseBoolean(Node.fromAttr(variantEl, "override"), false);
      tmpName = (override ? "" : tmpName + ": ") + variantEl.getTextNormalize();
    }

    this.name = tmpName;
    this.normalizedName = StringUtils.normalize(name);

    String slug = assertNotNull(root).getChildTextNormalize("slug");
    if (slug != null && variant != null) slug += "_" + variant;

    this.id = assertNotNull(StringUtils.slugify(slug != null ? slug : name));

    this.proto = assertNotNull(XMLUtils.parseSemanticVersion(Node.fromRequiredAttr(root, "proto")));
    this.version =
        assertNotNull(XMLUtils.parseSemanticVersion(Node.fromRequiredChildOrAttr(root, "version")));
    this.description =
        assertNotNull(
            Node.fromRequiredChildOrAttr(root, "objective", "description").getValueNormalize());
    this.created = XMLUtils.parseDate(Node.fromChildOrAttr(root, "created"));
    this.authors = parseContributors(root, "author");
    this.contributors = parseContributors(root, "contributor");
    this.rules = parseRules(root);
    this.difficulty =
        XMLUtils.parseEnum(
                Node.fromLastChildOrAttr(root, "difficulty"),
                Difficulty.class,
                "difficulty",
                Difficulty.NORMAL)
            .ordinal();
    this.tags = new TreeSet<>();
    this.players = new ArrayList<>();
    this.world = parseWorld(root);
    this.gamemode = XMLUtils.parseFormattedText(root, "game");
    this.gamemodes = parseGamemodes(root);
    this.phase =
        XMLUtils.parseEnum(
            Node.fromLastChildOrAttr(root, "phase"), Phase.class, "phase", Phase.PRODUCTION);
    this.friendlyFire =
        XMLUtils.parseBoolean(
            Node.fromLastChildOrAttr(root, "friendlyfire", "friendly-fire"), false);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getVariant() {
    return variant;
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
  public String getName() {
    return name;
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
    return tags;
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
    return "MapInfo{id=" + this.id + ", version=" + this.version + "}";
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
    if (style.showAuthors) {
      return translatable(
          "misc.authorship",
          NamedTextColor.DARK_PURPLE,
          name.build(),
          TextFormatter.list(
              getAuthors().stream()
                  .map(c -> c.getName(NameStyle.PLAIN).color(NamedTextColor.RED))
                  .collect(Collectors.toList()),
              NamedTextColor.DARK_PURPLE));
    }

    return name.build();
  }

  private static @NotNull List<String> parseRules(Element root) {
    List<String> rules = new ArrayList<>();
    for (Element parent : root.getChildren("rules")) {
      for (Element rule : parent.getChildren("rule")) {
        rules.add(rule.getTextNormalize());
      }
    }
    return rules;
  }

  private static @NotNull List<Gamemode> parseGamemodes(Element root) throws InvalidXMLException {
    List<Gamemode> gamemodes = new ArrayList<>();
    for (Element gamemodeEl : root.getChildren("gamemode")) {
      Gamemode gm = Gamemode.byId(gamemodeEl.getText());
      if (gm == null) throw new InvalidXMLException("Unknown gamemode", gamemodeEl);
      gamemodes.add(gm);
    }
    return gamemodes;
  }

  private static @NotNull List<Contributor> parseContributors(Element root, String tag)
      throws InvalidXMLException {
    List<Contributor> contributors = new ArrayList<>();
    for (Element parent : root.getChildren(tag + "s")) {
      for (Element child : parent.getChildren(tag)) {
        String name = XMLUtils.getNormalizedNullableText(child);
        UUID uuid = XMLUtils.parseUuid(Node.fromAttr(child, "uuid"));
        String contribution = XMLUtils.getNullableAttribute(child, "contribution", "contrib");

        if (name == null && uuid == null) {
          throw new InvalidXMLException("Contributor must have either a name or UUID", child);
        }

        if (uuid == null) {
          contributors.add(new PseudonymContributor(name, contribution));
        } else {
          contributors.add(new PlayerContributor(uuid, contribution));
        }
      }
    }
    return contributors;
  }

  private static @NotNull WorldInfo parseWorld(Element root) throws InvalidXMLException {
    final Element world = root.getChild("terrain");
    return world == null ? new WorldInfoImpl() : new WorldInfoImpl(world);
  }
}

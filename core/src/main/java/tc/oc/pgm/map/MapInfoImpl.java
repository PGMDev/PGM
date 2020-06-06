package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Difficulty;
import org.jdom2.Element;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.WorldInfo;
import tc.oc.pgm.map.contrib.PlayerContributor;
import tc.oc.pgm.map.contrib.PseudonymContributor;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class MapInfoImpl implements MapInfo {
  private final String id;
  private final Version proto;
  private final Version version;
  private final String name;
  private final String description;
  private final Collection<Contributor> authors;
  private final Collection<Contributor> contributors;
  private final Collection<String> rules;
  private final Component gamemode;
  private final int difficulty;
  private final WorldInfo world;

  protected final Collection<MapTag> tags;
  protected final Collection<Integer> players;

  public MapInfoImpl(
      @Nullable String id,
      Version proto,
      Version version,
      String name,
      String description,
      @Nullable Collection<Contributor> authors,
      @Nullable Collection<Contributor> contributors,
      @Nullable Collection<String> rules,
      @Nullable Integer difficulty,
      @Nullable Collection<MapTag> tags,
      @Nullable Collection<Integer> players,
      @Nullable WorldInfo world,
      Component gamemode) {
    this.name = checkNotNull(name);
    this.id = checkNotNull(MapInfo.normalizeName(id == null ? name : id));
    this.proto = checkNotNull(proto);
    this.version = checkNotNull(version);
    this.description = checkNotNull(description);
    this.authors = authors == null ? new LinkedList<>() : authors;
    this.contributors = contributors == null ? new LinkedList<>() : contributors;
    this.rules = rules == null ? new LinkedList<>() : rules;
    this.difficulty = difficulty == null ? Difficulty.NORMAL.ordinal() : difficulty;
    this.tags = tags == null ? new TreeSet<>() : tags;
    this.players = players == null ? new LinkedList<>() : players;
    this.world = world == null ? new WorldInfoImpl() : world;
    this.gamemode = gamemode == null ? TextComponent.empty() : gamemode;
  }

  public MapInfoImpl(MapInfo info) {
    this(
        checkNotNull(info).getId(),
        info.getProto(),
        info.getVersion(),
        info.getName(),
        info.getDescription(),
        info.getAuthors(),
        info.getContributors(),
        info.getRules(),
        info.getDifficulty(),
        info.getTags(),
        info.getMaxPlayers(),
        info.getWorld(),
        info.getGamemode());
  }

  public MapInfoImpl(Element root) throws InvalidXMLException {
    this(
        checkNotNull(root).getChildTextNormalize("slug"),
        XMLUtils.parseSemanticVersion(Node.fromRequiredAttr(root, "proto")),
        XMLUtils.parseSemanticVersion(Node.fromRequiredChildOrAttr(root, "version")),
        Node.fromRequiredChildOrAttr(root, "name").getValueNormalize(),
        Node.fromRequiredChildOrAttr(root, "objective", "description").getValueNormalize(),
        parseContributors(root, "author"),
        parseContributors(root, "contributor"),
        parseRules(root),
        XMLUtils.parseEnum(
                Node.fromLastChildOrAttr(root, "difficulty"),
                Difficulty.class,
                "difficulty",
                Difficulty.NORMAL)
            .ordinal(),
        null,
        null,
        parseWorld(root),
        XMLUtils.parseFormattedText(root, "game"));
  }

  @Override
  public String getId() {
    return id;
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
  public String getDescription() {
    return description;
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
  public WorldInfo getWorld() {
    return world;
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
    return new ToStringBuilder(this).append("id", getId()).build();
  }

  @Override
  public MapInfo clone() {
    return new MapInfoImpl(this);
  }

  @Override
  public Component getStyledName(MapNameStyle style) {
    TextComponent.Builder name = TextComponent.builder(getName());

    if (style.isColor) name.color(TextColor.GOLD);
    if (style.isHighlight) name.decoration(TextDecoration.UNDERLINED, true);
    if (style.showAuthors) {
      return TranslatableComponent.of(
          "misc.authorship",
          TextColor.DARK_PURPLE,
          name.build(),
          TextFormatter.list(
              getAuthors().stream()
                  .map(c -> c.getName(NameStyle.PLAIN).color(TextColor.RED))
                  .collect(Collectors.toList()),
              TextColor.DARK_PURPLE));
    }

    return name.build();
  }

  private static List<String> parseRules(Element root) {
    List<String> rules = null;
    for (Element parent : root.getChildren("rules")) {
      for (Element rule : parent.getChildren("rule")) {
        if (rules == null) {
          rules = new LinkedList<>();
        }
        rules.add(rule.getTextNormalize());
      }
    }
    return rules;
  }

  private static List<Contributor> parseContributors(Element root, String tag)
      throws InvalidXMLException {
    List<Contributor> contributors = null;
    for (Element parent : root.getChildren(tag + "s")) {
      for (Element child : parent.getChildren(tag)) {
        String name = XMLUtils.getNormalizedNullableText(child);
        UUID uuid = XMLUtils.parseUuid(Node.fromAttr(child, "uuid"));
        String contribution = XMLUtils.getNullableAttribute(child, "contribution", "contrib");

        if (name == null && uuid == null) {
          throw new InvalidXMLException("Contributor must have either a name or UUID", child);
        }

        if (contributors == null) {
          contributors = new LinkedList<>();
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

  private static WorldInfo parseWorld(Element root) throws InvalidXMLException {
    final Element world = root.getChild("terrain");
    return world == null ? new WorldInfoImpl() : new WorldInfoImpl(world);
  }
}

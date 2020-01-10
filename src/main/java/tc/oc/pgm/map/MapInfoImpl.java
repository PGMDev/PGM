package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.map.contrib.PlayerContributor;
import tc.oc.pgm.map.contrib.PseudonymContributor;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.server.NullCommandSender;
import tc.oc.util.SemanticVersion;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class MapInfoImpl implements MapInfo {

  private final String id;
  private final SemanticVersion proto;
  private final SemanticVersion version;
  private final String name;
  private final String genre;
  private final String description;
  private final Collection<Contributor> authors;
  private final Collection<Contributor> contributors;
  private final Collection<String> rules;
  private final Difficulty difficulty;
  private final Collection<Integer> teamLimits;
  private final int playerLimit;

  public MapInfoImpl(
      @Nullable String id,
      SemanticVersion proto,
      SemanticVersion version,
      String name,
      String genre,
      String description,
      Collection<Contributor> authors,
      Collection<Contributor> contributors,
      Collection<String> rules,
      Difficulty difficulty,
      Collection<Integer> teamLimits,
      int playerLimit) {
    this.proto = checkNotNull(proto);
    this.version = checkNotNull(version);
    this.name = checkNotNull(name);
    this.genre = checkNotNull(genre);
    this.description = checkNotNull(description);
    this.authors = ImmutableList.copyOf(checkNotNull(authors));
    this.contributors = ImmutableList.copyOf(checkNotNull(contributors));
    this.rules = ImmutableList.copyOf(checkNotNull(rules));
    this.difficulty = checkNotNull(difficulty);
    this.teamLimits = ImmutableList.copyOf(checkNotNull(teamLimits));
    this.playerLimit = Math.max(1, playerLimit);
    this.id = checkNotNull(normalizeName(id == null ? this.name : id));
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public SemanticVersion getProto() {
    return proto;
  }

  @Override
  public SemanticVersion getVersion() {
    return version;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getGenre() {
    return genre;
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
  public Difficulty getDifficulty() {
    return difficulty;
  }

  @Override
  public Collection<Integer> getTeamLimits() {
    return teamLimits;
  }

  @Override
  public int getPlayerLimit() {
    return playerLimit;
  }

  @Override
  public int compareTo(MapInfo o) {
    return new CompareToBuilder()
        .append(getName(), o.getName())
        .append(getVersion(), o.getVersion())
        .append(getId(), o.getId())
        .build();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getId()).append(getVersion()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapInfo)) return false;
    final MapInfo o = (MapInfo) obj;
    return new EqualsBuilder()
        .append(getId(), o.getId())
        .append(getVersion(), o.getVersion())
        .build();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("name", getName())
        .append("version", getVersion())
        .build();
  }

  public static MapInfo parseInfo(Document doc) throws InvalidXMLException {
    final Element root = doc.getRootElement();

    final SemanticVersion proto =
        XMLUtils.parseSemanticVersion(Node.fromRequiredAttr(root, "proto"));
    if (proto.isNewerThan(PGM.get().getMapLibrary().getProto())) {
      throw new InvalidXMLException("Unsupported map proto: " + proto, root);
    }

    final Collection<Contributor> authors = parseContributors(root, "author");
    if (authors.isEmpty()) {
      throw new InvalidXMLException("At least 1 author must be specified", root);
    }

    final String id = root.getChildTextNormalize("slug");
    final SemanticVersion version =
        XMLUtils.parseSemanticVersion(Node.fromRequiredChildOrAttr(root, "version"));
    final String name = Node.fromRequiredChildOrAttr(root, "name").getValueNormalize();
    final String description =
        Node.fromRequiredChildOrAttr(root, "objective", "description").getValueNormalize();

    Component genre =
        XMLUtils.parseFormattedText(
            root, "game", new PersonalizedTranslatable("match.scoreboard.default.title"));
    if (genre != null) {
      final Element blitz = root.getChild("blitz");
      if (blitz != null) {
        final Element title = blitz.getChild("title");
        if (title != null) {
          if (proto.isNoOlderThan(ProtoVersions.REMOVE_BLITZ_TITLE)) {
            throw new InvalidXMLException(
                "<title> inside <blitz> is no longer supported, use <map game=\"...\">", title);
          }
          genre = new PersonalizedText(title.getTextNormalize());
        }
      }
    }
    final String genreString = genre.render(NullCommandSender.INSTANCE).toLegacyText();

    final Difficulty difficulty =
        XMLUtils.parseEnum(
            Node.fromLastChildOrAttr(root, "difficulty"), Difficulty.class, "difficulty");
    final Collection<Contributor> contributors = parseContributors(root, "contributor");

    final List<String> rules = new LinkedList<>();
    for (Element parent : root.getChildren("rules")) {
      for (Element rule : parent.getChildren("rule")) {
        rules.add(rule.getTextNormalize());
      }
    }

    // FIXME: make dynamic
    final Collection<Integer> teamLimits = new LinkedList<>();
    final int playerLimit = Bukkit.getMaxPlayers();

    return new MapInfoImpl(
        id,
        proto,
        version,
        name,
        genreString,
        description,
        authors,
        contributors,
        rules,
        difficulty,
        teamLimits,
        playerLimit);
  }

  private static List<Contributor> parseContributors(Element root, String tag)
      throws InvalidXMLException {
    List<Contributor> contributors = new LinkedList<>();
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

  public static String normalizeName(@Nullable String idOrName) {
    return idOrName == null
        ? ""
        : Normalizer.normalize(idOrName, Normalizer.Form.NFD)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase();
  }
}

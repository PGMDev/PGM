package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.util.TranslationUtils;
import tc.oc.util.SemanticVersion;
import tc.oc.util.StringUtils;
import tc.oc.util.components.Components;

/** Class describing the match-independent information about a map. */
public class MapInfo {

  public final String id;

  public final SemanticVersion proto;

  /** Name of the map. */
  public final String name;

  public final SemanticVersion version;

  /** Optional game name to override the default */
  public final @Nullable Component game;

  /** Short, one-line description of the objective of this map. */
  public final String objective;

  /** List of authors and their contributions. */
  public final List<Contributor> authors;

  /** List of contributors and their contributions. */
  public final List<Contributor> contributors;

  /** List of rules for this map. */
  public final List<String> rules;

  /** Difficulty the map should be played on. */
  public final @Nullable Difficulty difficulty;

  /** Dimension the map should be loaded in */
  public final Environment dimension;

  /** Whether friendly fire should be on or off. */
  public final boolean friendlyFire;

  public MapInfo(
      SemanticVersion proto,
      @Nullable String slug,
      String name,
      SemanticVersion version,
      @Nullable Component game,
      String objective,
      List<Contributor> authors,
      List<Contributor> contributors,
      List<String> rules,
      @Nullable Difficulty difficulty,
      Environment dimension,
      boolean friendlyFire) {

    this.id = slug != null ? slug : name.toLowerCase().replaceAll("[^a-z\\d]", "_");

    this.proto = checkNotNull(proto);
    this.name = checkNotNull(name);
    this.version = checkNotNull(version);
    this.game = game;
    this.objective = checkNotNull(objective);
    this.authors = checkNotNull(authors);
    this.contributors = checkNotNull(contributors);
    this.rules = checkNotNull(rules);
    this.difficulty = difficulty;
    this.dimension = checkNotNull(dimension);
    this.friendlyFire = friendlyFire;
  }

  public String slug() {
    return id;
  }

  public String getFormattedMapTitle() {
    return StringUtils.dashedChatMessage(
        ChatColor.DARK_AQUA + " " + this.name + ChatColor.GRAY + " " + version,
        "-",
        ChatColor.RED + "" + ChatColor.STRIKETHROUGH);
  }

  public String getShortDescription(CommandSender sender) {
    String text = ChatColor.GOLD + this.name;

    List<Contributor> authors = getNamedAuthors();
    if (!authors.isEmpty()) {
      text =
          AllTranslations.get()
              .translate(
                  (input) -> ChatColor.DARK_PURPLE + input,
                  "misc.authorship",
                  sender,
                  text,
                  TranslationUtils.legacyList(
                      sender,
                      (input) -> ChatColor.DARK_PURPLE + input,
                      (input) -> ChatColor.RED + input,
                      authors));
    }

    return text;
  }

  /** Apply standard formatting (aqua + bold) to the map name */
  public String getColoredName() {
    return ChatColor.AQUA.toString() + ChatColor.BOLD + this.name;
  }

  /** Apply standard formatting (aqua + bold) to the map version */
  public String getColoredVersion() {
    return ChatColor.DARK_AQUA.toString() + ChatColor.BOLD + version;
  }

  public List<Contributor> getNamedAuthors() {
    return Contributor.filterNamed(this.authors);
  }

  public List<Contributor> getNamedContributors() {
    return Contributor.filterNamed(this.contributors);
  }

  public Iterable<Contributor> getAllContributors() {
    return Iterables.concat(authors, contributors);
  }

  public boolean isAuthor(UUID player) {
    for (Contributor author : authors) {
      if (player.equals(author.getPlayerId())) return true;
    }
    return false;
  }

  public Component getLocalizedGenre() {
    return new PersonalizedTranslatable("map.genre.other");
  }

  public Component getLocalizedEdition() {
    return Components.blank();
  }
}

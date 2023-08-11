package tc.oc.pgm.join;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;

public class JoinRequest {
  private static final JoinRequest FORCE = new JoinRequest(null, 1, EnumSet.of(Flag.FORCE));
  private static final JoinRequest EMPTY = new JoinRequest(null, 1, EnumSet.noneOf(Flag.class));

  private final @Nullable Team team;
  private final int players;
  private final ImmutableSet<Flag> flags;

  private JoinRequest(@Nullable Team team, int players, Set<Flag> flags) {
    this.team = team;
    this.players = players;
    this.flags = ImmutableSet.copyOf(flags);
  }

  public static JoinRequest of(@Nullable Team team, Flag... flags) {
    return new JoinRequest(team, 1, toSet(flags));
  }

  public static JoinRequest of(@Nullable Team team, Set<Flag> flags) {
    return new JoinRequest(team, 1, flags);
  }

  public static JoinRequest fromPlayer(MatchPlayer player, @Nullable Team team, Flag... extra) {
    return of(team, playerFlags(player, extra));
  }

  public static JoinRequest group(@Nullable Team team, int size, Set<Flag> flags) {
    return new JoinRequest(team, size, flags);
  }

  public static JoinRequest empty() {
    return EMPTY;
  }

  public static JoinRequest force() {
    return FORCE;
  }

  @Nullable
  public Team getTeam() {
    return team;
  }

  public int getPlayerCount() {
    return players;
  }

  public boolean has(Flag flag) {
    return flags.contains(flag);
  }

  public boolean isForcedOr(Flag flag) {
    return flags.contains(Flag.FORCE) || flags.contains(flag);
  }

  public enum Flag {
    JOIN,
    JOIN_CHOOSE,
    JOIN_FULL,
    SQUAD, // Joining as a squad/party
    IGNORE_QUEUE, // If there's a join queue, ignore it. Used for queued joins actually joining in
    SHOW_TITLE, // Show a title after joining
    FORCE // Bypass any kind of restriction (eg: not joining during blitz)
  }

  public static EnumSet<Flag> playerFlags(MatchPlayer player, Flag... extra) {
    EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
    if (player.getBukkit().hasPermission(Permissions.JOIN)) flags.add(Flag.JOIN);

    if (player.getBukkit().hasPermission(Permissions.JOIN_CHOOSE)) flags.add(Flag.JOIN_CHOOSE);

    if (player.getBukkit().hasPermission(Permissions.JOIN_FULL)) flags.add(Flag.JOIN_FULL);

    flags.addAll(Arrays.asList(extra));

    return flags;
  }

  private static EnumSet<Flag> toSet(Flag... flags) {
    return flags == null || flags.length == 0
        ? EnumSet.noneOf(Flag.class)
        : EnumSet.copyOf(Arrays.asList(flags));
  }
}

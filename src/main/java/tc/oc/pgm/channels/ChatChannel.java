package tc.oc.pgm.channels;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.Party;

public class ChatChannel {

  private final Set<MatchPlayer> members = new HashSet<>();
  private final Party party;

  public ChatChannel(@Nullable Party party) {
    this.party = party;
  }

  public @Nullable Party getParty() {
    return party;
  }

  // TODO: mutes and chat events
  public boolean sendMessage(MatchPlayer member, String rawMessage) {
    members.forEach(
        m ->
            m.sendMessage(
                new PersonalizedText(
                    party.getChatPrefix().toLegacyText()
                        + member.getDisplayName(m)
                        + ": "
                        + ChatColor.stripColor(rawMessage))));
    return true;
  }

  public void addMember(MatchPlayer member) {
    members.add(member);
  }

  public void removeMember(MatchPlayer member) {
    members.remove(member);
  }

  public void clearMembers() {
    members.clear();
  }
}

package tc.oc.pgm.channels;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.MatchScope;
import tc.oc.pgm.match.MultiPlayerParty;
import tc.oc.pgm.match.Party;
import tc.oc.pgm.module.ModuleLoadException;

@ListenerScope(MatchScope.LOADED)
public class ChannelsMatchModule extends MatchModule implements Listener {
  public static class Factory implements MatchModuleFactory<ChannelsMatchModule> {
    @Override
    public ChannelsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ChannelsMatchModule(match);
    }
  }

  private Map<Party, ChatChannel> partyToChannel = new HashMap<>();
  private ChatChannel global = new ChatChannel(null);

  public ChannelsMatchModule(Match match) {
    super(match);
  }

  public ChatChannel getGlobalChannel() {
    return global;
  }

  private void addMember(MatchPlayer member) {
    Party party = member.getParty();
    partyToChannel.putIfAbsent(member.getParty(), new ChatChannel(party));
    partyToChannel.get(party).addMember(member);
  }

  private void removeMember(MatchPlayer member) {
    Party party = member.getParty();
    partyToChannel.putIfAbsent(member.getParty(), new ChatChannel(party));
    partyToChannel.get(party).removeMember(member);
  }

  protected void clear() {
    partyToChannel.values().forEach(ChatChannel::clearMembers);
    partyToChannel.clear();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onAsyncChat(AsyncPlayerChatEvent event) {
    if (event.getPlayer() == null || event.getMessage() == null) {
      return;
    }
    MatchPlayer member = match.getPlayer(event.getPlayer());
    String raw = event.getMessage();
    partyToChannel.get(member.getParty()).sendMessage(member, raw);
    event.setCancelled(true);
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    Party oldParty = event.getOldParty();
    Party newParty = event.getNewParty();
    MatchPlayer member = event.getPlayer();

    if (oldParty != null && newParty != null) {
      removeMember(member);
      addMember(member);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    this.addMember(event.getPlayer());
  }

  @EventHandler
  public void onPlayerLeave(PlayerLeaveMatchEvent event) {
    this.removeMember(event.getPlayer());
  }

  @Override
  public void load() {
    match.getParties().stream()
        .filter(party -> party instanceof MultiPlayerParty)
        .forEach(party -> partyToChannel.put(party, new ChatChannel(party)));
  }

  @Override
  public void enable() {
    this.load();
    if (match.hasMatchModule(FreeForAllMatchModule.class)) {
      return;
    }
    match.getPlayers().forEach(this::addMember);
  }

  @Override
  public void disable() {
    this.clear();
  }
}

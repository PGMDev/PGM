## 0.8 (Unreleased)

### Admin tools
- /setpool has been added. Use it to edit map pools while the server is running.
- /vanish has been added. This command makes you disappear from sight to non-staff members, and sends fake leaving/joining messages when you vanish and unvanish. If you join a server that starts with a "vanish." subdomain, you'll automatically be vanished on login.
- MySQL database support has been added.
- Map rotations now continue to progress across server restarts.
- Permissions can be customized for match observers and participants.
- The "community" module of PGM can now be disabled. Server owners may want to do this if they wish to use other plugins to provide the "community" features.

### New XML modules
- A spawner module has been added to the XML, which replicates the functionality of native Minecraft spawners. Modifying the NBT data of spawners can be confusing, so hopefully this module will make it easier to spawn entities, items, and potion effects on regular intervals.
- The gamerule XML module has been added back in. This allows map-makers to set gamerules for a map to arbitrary values.

### Other improvements
- /prox is now an alias for /proximity.

### Bug fixes
- Fire AsyncPlayerChatEvent for global, team, and admin chat.
- Fix rendering exception with relative durations.
- Fix control point scores rounding down to zero.
- Fix missing unit conversion for periodic tasks.
- "/help is null" bug fixed.
- Matches that end after a time limit now have the scoreboard updated properly.
- Matches that end with a time limit should declare the correct winner more frequently.
- Vehicles are now frozen when the match ends.
- An exception is no longer thrown on startup if the first map isn't loaded.
- Fixed some tab list glitches.
- Pre-match loading has been improved.
- Fixed some memory leaks.
- A bunch of moderation bugs have been fixed.
- /report now lists the correct sender for staff.
- Console messages now show the appropriate plugin prefix.
- The maximum build height is now properly applied in wool rooms.
- Match stats are no longer being sent to players who have disabled them.
## 0.8 (Unreleased)

### Features
- Rotations continue to work across server restarts
- /prox is now an alias for /proximity
- /setpool has been added, allows you to modify map pools on-the-fly
- Support for MySQL databases
- Can now customize permissions for match observers and participants
- /vanish has been added
- A spawner module has been added to the XML
- The gamerule XML module has been added back in
- The "community" features can now be disabled if you'd like to use other plugins instead

### Fixes
- Fire AsyncPlayerChatEvent for global, team, and admin chat
- Fix rendering exception with relative durations
- Fix control point scores rounding down to zero
- Fix missing unit conversion for periodic tasks
- "/help is null" bug fixed
- Matches that end after a time limit now have the scoreboard updated properly
- Matches that end with a time limit should declare the correct winner more frequently
- Vehicles are now frozen when the match ends
- An exception is no longer thrown on startup if the first map isn't loaded
- Fixed some tab list glitches
- Pre-match loading has been improved
- Fixed some memory leaks
- A bunch of moderation bugs have been fixed
- /report now lists the correct sender for staff

### Refactored
- Additional annotations have been added
- org.joda.time is no longer a dependency, we now use java.time
- Dependency on NMSHacks has been reduced
- The ItemTag API has been recoded to be compatible across versions of Minecraft
- Major work to move away from SportPaper API. We're now using a new text library instead of the old SportPaper 
components.
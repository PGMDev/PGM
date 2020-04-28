Translations [![Crowdin](https://badges.crowdin.net/pgm/localized.svg)](https://crowdin.com/project/pgm)
===

All languages that are [supported by the Minecraft client](https://crowdin.net/project/minecraft) are supported by PGM.

The original English string templates live in [strings.properties](templates/strings.properties), and may be edited
when making modifications to the project. Modifications to the English string templates will be automatically uploaded
to [our CrowdIn project](https://crowdin.com/project/pgm) when a new commit is pushed to the `master` branch or any
`feat/*` branch on the main PGM repo. Translations may then be submitted by the community. Once they are approved, they
will be automatically PR'd back into the source branch on an hourly basis.


**Note**: Please do not modify any *translation* files directly in this repo. Instead, translations should be submitted
through [our CrowdIn project](https://crowdin.com/project/pgm), through the process described above.

Working with output
===

Whenever you want to add some function that gives the user(s) some text response, instead of hardcoding in the response
like this: ``audience.sendMessage("You won!");``  
You should add your string to ```strings.properties``` with a key and reference that key
in your code using ```TranslatableComponent```like this:
``àudience.sendMessage(TranslatableComponent.of("player.won"))``  
For more features like colors see [KyoriPowered/text](https://github.com/KyoriPowered/text)

Naming keys
===

When adding a string to ```strings.properties``` you need to give it a  _key_. A key consists of multiple keywords e.g:  
`firstKeyword.secondKeyword.thirdKeyword`

The first keyword should always tell which category the string belongs to. A list over existing categories will be at
the bottom of this README.

The second keyword should tell which function it belongs to.  
`moderation.freeze` <- Here the second word explains that the string is part of the _freeze_ function in moderation.

Any keyword after the second one is only necessary if there are multiple strings that uses the same second keyword.  
`moderation.freeze.frozen` `moderation.freeze.unfrozen`

You can add a fourth keyword if multiple strings has to use the same third keyword. 
`core.touch.owned.you` `core.touch.owned.player`   
But any higher amount of words per key should be avoided if possible.

Adding context
===

If a string has any variables  
`moderation.freeze.freeze = You have frozen {0}`

A comment should be added in the lines above like this  
`# {0} = the player frozen`  
`moderation.freeze.freeze = You have frozen {0}`

Each variable should have its own comment explaining it  
`# {0} = ...`  
`# {1} = ...`  
`# {n} = ...`  
`keyword.kingword = ...`

You can of course add other context in the comments as well, but it is mostly not necessary

Existing categories
===
admin - Administrative functions not handling players  
system - System messages  
flag - All functions working with flags  
wool - All functions working with wools  
chat - Chat-related functions like channels and private messages  
class - All functions working with classes  
command - Generic strings that apply to many things regarding commands  
core - All functions working with cores   
destroyable - All functions working with destroyables (also known as monuments)  
death - All strings used by the death message builder  
misc - generic strings that can be used multiple places  
gamemode - All functions working with gamemodes not big enough to have their own categories  
setting - All functions working with settings (This has its own category as preparation for [#217](https://github.com/Electroid/PGM/issues/217))  
observer - All functions used exclusively by observers  
maps - All functions working with the maps used without affecting the match  
match - All functions administering the matches  
moderation - Administrative functions handling players  
stats - All functions working with statistics
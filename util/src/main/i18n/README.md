Translations [![Crowdin](https://badges.crowdin.net/pgm/localized.svg)](https://crowdin.com/project/pgm)
===

PGM uses [Crowdin](https://crowdin.com/project/pgm) to maintain its extensive amount of translations. Any message, alert, or user interface should be properly translated using this guide. See the `i18n/templates` folder for a comprehensive list of all translation keys.

If you want to add a new translation key or editing an existing key **in English** make a PR on Github. For all other languages, go to our project on [Crowdin](https://crowdin.com/project/pgm).

Example
===

Let's say you want to send a message to player's telling them who won the match.

First, go to `i18n/templates/match.properties` and add a new key. Always make sure to add `{0} = ...` comments above the key so others know how to translate it.

```properties
# {0} = player name
# {1} = number of points
match.end.notifyWinner = Congrats, {0} won the match with {1} points!
```

Then, go to your code and use `TranslatableComponent` and `TextComponent` to create your message. 

```java
public void sendMatchEnd(MatchPlayer player, String winnerName, int points) {
  player.sendMessage(
    TranslatableComponent.of("match.end.notifyWinner",
      TextComponent.of(winnerName, TextColor.GOLD),
      TextComponent.of(points, TextColor.GREEN),
      TextColor.AQUA));
}
```

See `TextFormatter` for extra utilities such as translating lists, multiple names, or time durations.

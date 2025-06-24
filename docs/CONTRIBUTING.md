Contributing
===========

We're really excited to have you join the community of contributors for the PGM project! Before we jump into the nitty-gritty details, let's go over the design choices and philosophy of the project so we're all on the same page.

Scope
-----

PvP Game Manager, as the name implies, should focus on providing matchmaking and gameplay functionality rather than administrative features, which other plugins support. There are some exceptions, such as chat or settings, that require explicit interoperability with PGM.

Philosophy
----------

1. [**Simplicity**](https://thevaluable.dev/kiss-principle-explained/)

The most important principle is maintaining simplicity, both for contributors and players. When thinking about your changes, make sure your code is easy to comprehend, not overly complex or abstracted, and doesn't include unnecessary third-party libraries. If your changes affect gameplay, make sure players can easily adapt to them.

2. [**Collaboration**](https://deepsource.io/blog/code-review-best-practices/)

You can save a good amount of time during code review if you share your ideas and gameplan *before* writing any code. We encourage submitting [draft](https://github.blog/2019-02-14-introducing-draft-pull-requests/) pull requests, even if your code doesn't compile or work, to get feedback on the design of your changes. Break up your changes into smaller, incremental pull requests with a "soft" max of 200 lines changed.

3. [**Out-of-the-box**](https://www.smithsonianmag.com/arts-culture/how-steve-jobs-love-of-simplicity-fueled-a-design-revolution-23868877/)

Players, contributors, and server owners should be able to enjoy the core mechanics of PGM "out-of-the-box" without any extra plugins or requirements. No website, database, or external API should be required to run a server on your local machine. Eventually, it should be able to run on any Minecraft server version and be compatible with the most popular Bukkit plugins.

Dependencies
------------
You'll need to make sure your machine has the following dependencies before compiling PGM. The instructions will vary by operation system, so click on each link to read the detailed steps.

 * [Java 21](https://docs.oracle.com/en/java/javase/21/install/overview-jdk-installation.html) - the Java Virtual Machine required to run the Minecraft server and PGM plugin.
 * [Gradle](https://gradle.org/install/) - a build tool to bundle all the Java dependencies into `.jar` file

Steps
---------

1. Clone the repository on your machine.

```bash
git clone git@github.com:PGMDev/PGM.git
```

2. Make your changes (league system, anyone?)

3. Run the code formatter, we follow [palantir java format](https://github.com/palantir/palantir-java-format), based on google's [code style.](https://google.github.io/styleguide/javaguide.html)

```bash
./gradlew spotlessApply
```

4. Compile a new version with your changes.
```bash
./gradlew build # creates .jar in build/libs which you would copy to your plugins folder
```
Note: If you wish to use your modified PGM as a dependency, `./gradlew publishToMavenLocal` will install the build in your local maven repository


5. Commit your changes, using the `-S` and `-s` tag to [sign](https://help.github.com/en/github/authenticating-to-github/signing-commits) and [certify](https://developercertificate.org) the origin of your code.
```bash
git commit -S -s -m "A short description of your changes"
```

6. Submit your pull request for review and feedback.

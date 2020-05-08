package tc.oc.pgm.map.source;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;

public class GitMapSourceFactory extends SystemMapSourceFactory {

  private final String gitURI;
  private final Logger logger;
  private final File dir;
  //The factory needs to parse the source differently depending on if credentials are provided
  private static boolean credentialsProvided;
  private final boolean updateOnRestart;

  private CredentialsProvider provider = null;

  private static File getFile(String source) {
    credentialsProvided = source.contains(";");

    int uniqueIdentifier = Math.abs(source.hashCode()); //Prevents errors on similar repository names

    String dirName;
    if (credentialsProvided)
      dirName =
          (source.substring(source.lastIndexOf("/"), source.indexOf(";")) + "_" + uniqueIdentifier);
    else dirName = (source.substring(source.lastIndexOf("/") + 1) + "_" + uniqueIdentifier);

    return new File("./gitMaps/" + dirName);
  }

  public GitMapSourceFactory(String source, Logger logger, boolean updateOnRestart)
      throws MapMissingException, ClassNotFoundException {
    super(getFile(source).getPath());

    if (credentialsProvided) this.gitURI = source.substring(0, source.indexOf(";"));
    else this.gitURI = source;

    this.logger = logger;
    this.dir = getFile(source);
    this.updateOnRestart = updateOnRestart;

    //Disable if JGit is not present
    if (Class.forName("org.eclipse.jgit.api.Git") == null) {
      throw new MapMissingException(
          dir.getPath(),
          "Unable to load JGit(was it excluded when compiling the jar?)");
    }

    File masterDir = new File("./gitMaps");
    if (!masterDir.exists()) masterDir.mkdir();

    // Ensure the git map directory exists before trying to update it
    if (!dir.exists()) dir.mkdir();
    if (!dir.isDirectory())
      throw new MapMissingException(
          dir.getPath(), "Unable to read git directory " + dir.getName() + "(Is it a file?)");

    if (credentialsProvided) {
      provider =
          new UsernamePasswordCredentialsProvider(
              source.substring(source.indexOf(";") + 1, source.lastIndexOf(";")),
              source.substring(source.lastIndexOf(";") + 1));
    }
  }

  @Override
  public Iterator<? extends MapSource> loadNewSources() throws MapMissingException {
    final Stream<String> paths;

    try {
      if (updateOnRestart) refreshRepo();
    } catch (GitAPIException e) {
      logger.log(
          Level.WARNING,
          "Unable to fetch repo " + dir.getName() + ", caused by: " + e.getMessage());
      if (e instanceof TransportException) {
        logger.log(Level.INFO, "Provide your username and password in the config like this:");
        logger.log(Level.INFO, "URL;USERNAME;PASSWORD");
        logger.log(Level.INFO, "e.g: Github.com/KingOfSquares/maps;KingOfSquares;password1234");
      }
    }

    //Load maps separately so it still loads downloaded maps if git connection fails
    try {
      paths = loadAllPaths();
    } catch (IOException e) {
      throw new MapMissingException(dir.getPath(), "Unable to list files in" + dir.getName(), e);
    }

    return paths.parallel().flatMap(this::loadNewSource).iterator();
  }

  private void refreshRepo() throws GitAPIException, MapMissingException {
    Git git;
    logger.log(Level.INFO, "Refreshing local git repository " + dir.getName() + "...");

    try {
      git = Git.open(dir);
    } catch (IOException e) {
      logger.log(
          Level.INFO,
          "Unable to open local git repository "
              + dir.getName()
              + ", trying to clone remote git repository...");
      logger.log(
          Level.INFO, "This will happen the first time PGM tries to read a new git repository");
      try {

        CloneCommand clone = Git.cloneRepository().setDirectory(dir).setURI(gitURI);
        if (provider != null) clone.setCredentialsProvider(provider);
        git = clone.call();
        logger.log(Level.INFO, "Successfully cloned git repository " + dir.getName() + "!");
      } catch (GitAPIException f) {
        f.printStackTrace();
        throw new MapMissingException(
            dir.getPath(),
            "Unable to connect to remote git repo " + dir.getName() + ", is the URI correct?",
            e);
      }
    }
    git.clean().call();

    FetchCommand fetch = git.fetch();
    if (provider != null) fetch.setCredentialsProvider(provider);
    fetch.call();

    git.reset().setRef("@{upstream}").setMode(ResetCommand.ResetType.HARD).call();
  }
}

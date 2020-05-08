package tc.oc.pgm.map.source;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;

public class GitMapSourceFactory extends SystemMapSourceFactory {

  private final String gitURI;
  private final Logger logger;
  private final File dir;

  private static File getFile(String uri) {
    String dirName = uri.substring(uri.lastIndexOf("/"));

    return new File("./gitMaps/" + dirName);
  }

  public GitMapSourceFactory(String uri, Logger logger) throws MapMissingException {
    super(getFile(uri).getPath());

    this.gitURI = uri;
    this.logger = logger;
    this.dir = getFile(uri);

    File masterDir = new File("./gitMaps");
    if (!masterDir.exists()) masterDir.mkdir();

    // Ensure the git map directory exists before trying to update it
    if (!dir.exists()) dir.mkdir();
    if (!dir.isDirectory())
      throw new MapMissingException(
          dir.getPath(), "Unable to read git directory " + dir.getName() + "(Is it a file?)");
  }

  @Override
  public Iterator<? extends MapSource> loadNewSources() throws MapMissingException {
    final Stream<String> paths;

    try {
      // Class.forName("org.eclipse.jgit");

      // loadNewSources is executed on server startup, so no reason to reference this method
      // elsewhere
      refreshRepo();

      paths = loadAllPaths();
    }

    // catch (ClassNotFoundException e) {
    //  throw new MapMissingException(dir.getPath(),
    //     "Unable to load JGit classes(did you exclude them when compiling the jar?)", e);
    // }

    catch (GitAPIException e) {
      throw new MapMissingException(dir.getPath(), "Unable to fetch repo " + dir.getName(), e);
    } catch (IOException e) {
      throw new MapMissingException(dir.getPath(), "Unable to list files " + dir.getName(), e);
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
        git = Git.cloneRepository().setDirectory(dir).setURI(gitURI).call();
        logger.log(Level.INFO, "Successfully cloned git repository " + dir.getName() + "!");
      } catch (GitAPIException f) {
        f.printStackTrace();
        throw new MapMissingException(
            dir.getPath(),
            "Unable to connect to remote git repo "
                + dir.getName()
                + ", is the URI correct and the repository public?",
            e);
      }
    }
    git.clean().call();
    git.fetch().call();
    git.reset().setRef("@{upstream}").setMode(ResetCommand.ResetType.HARD).call();
  }
}

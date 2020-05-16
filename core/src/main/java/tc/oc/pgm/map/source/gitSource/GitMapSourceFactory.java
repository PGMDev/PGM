package tc.oc.pgm.map.source.gitSource;

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
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.map.source.SystemMapSourceFactory;

public class GitMapSourceFactory extends SystemMapSourceFactory {

  private final URIish gitURI;
  private final Logger logger;
  private final File dir;
  private final GitRepository repository;

  private CredentialsProvider provider = null;

  public GitMapSourceFactory(GitRepository repo, Logger logger) throws MapMissingException {
    super(repo.getDir().getPath());

    this.repository = repo;
    this.gitURI = repository.getUri();

    this.logger = logger;
    this.dir = repo.getDir();

    File masterDir = new File("./maps");
    if (!masterDir.exists()) masterDir.mkdir();

    // Ensure the git map directory exists before trying to update it
    if (!dir.exists()) {
      dir.mkdir();
    }
    if (!dir.isDirectory())
      throw new MapMissingException(
          dir.getPath(),
          "Unable to read git directory " + gitURI.getHumanishName() + "(Is it a file?)");

    if (repo.getUri().getPass() != null) {
      provider =
          new UsernamePasswordCredentialsProvider(repo.getUri().getUser(), repo.getUri().getPass());
    }
  }

  @Override
  public Iterator<? extends MapSource> loadNewSources() {

    return new Iterator<MapSource>() {
      private Iterator<MapSource> delegate;

      @Override
      public boolean hasNext() {
        if (delegate == null) {
          try {
            refreshRepo();
          } catch (GitAPIException e) {
            logger.log(
                Level.WARNING,
                "Unable to fetch repo " + gitURI.getPath() + ", caused by: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof TransportException) {
              logger.log(
                  Level.INFO, "Wrong username and password provided for " + gitURI.getPath());
            }
          } catch (MapMissingException e) {
            e.printStackTrace();
          }
          Stream<String> paths;

          // Load maps separately so it still loads downloaded maps if git connection fails
          try {
            paths = loadAllPaths();
          } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to list files in" + dir.getName(), e);
            paths = Stream.empty();
          }
          delegate = paths.parallel().flatMap((s) -> loadNewSource(s)).iterator();
        }
        return delegate.hasNext();
      }

      @Override
      public MapSource next() {
        return delegate.next();
      }
    };
  }

  private void refreshRepo() throws GitAPIException, MapMissingException {
    Git git;
    logger.log(Level.INFO, "Refreshing local git repository " + gitURI.getPath() + "...");

    try {
      git = Git.open(dir);
    } catch (IOException e) {
      logger.log(
          Level.INFO,
          "Unable to open local git repository "
              + gitURI.getPath()
              + ", trying to clone remote git repository...");
      logger.log(
          Level.INFO, "This will happen the first time PGM tries to read a new git repository");
      try {

        CloneCommand clone = Git.cloneRepository().setDirectory(dir).setURI(gitURI.toString());
        clone.setCredentialsProvider(provider);
        git = clone.call();
        changeBranch(git);

        logger.log(Level.INFO, "Successfully cloned git repository " + gitURI.getPath() + "!");
        return;
      } catch (GitAPIException f) {
        f.printStackTrace();
        throw new MapMissingException(
            dir.getPath(),
            "Unable to connect to remote git repo " + gitURI.toString() + ", is the URI correct?",
            f);
      }
    }
    git.clean().call();

    FetchCommand fetch = git.fetch();
    fetch.setCredentialsProvider(provider);
    fetch.call();

    changeBranch(git);

    git.reset().setMode(ResetCommand.ResetType.HARD).call();
  }

  private void changeBranch(Git git) throws GitAPIException {
    if (repository.getBranch() != null) {
      git.checkout().setName("origin/" + repository.getBranch()).call();
    }
  }
}

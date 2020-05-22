package tc.oc.pgm.map.source.gitSource;

import static tc.oc.pgm.util.text.TextException.configError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.kyori.text.TextComponent;
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
import tc.oc.pgm.util.text.TextException;

public class GitMapSourceFactory extends SystemMapSourceFactory {

  private final URIish gitURI;
  private final File dir;
  private final GitRepository repository;

  private CredentialsProvider provider = null;

  public GitMapSourceFactory(GitRepository repo) throws MapMissingException {
    super(repo.getDir().getPath());

    this.repository = repo;
    this.gitURI = repository.getUri();

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
  public Iterator<? extends MapSource> loadNewSources() throws MapMissingException {
    try {
      refreshRepo();
    } catch (GitAPIException | TextException e) {
      e.printStackTrace();
    }
    if (repository.getFolders() == null) return super.loadNewSources();
    System.out.println(repository.getFolders());

    List<MapSource> sources = new ArrayList<>();
    for (String folder : repository.getFolders()) {
      File sourcesFolder = new File(dir.getPath() + "/" + folder);
      System.out.println(sourcesFolder.getName());
      if (!sourcesFolder.isDirectory() || !sourcesFolder.exists()) continue;
      for (File map : sourcesFolder.listFiles()) {
        sources.add(super.loadSource(map.getPath()));
      }
    }
    return sources.iterator();
  }

  private void refreshRepo() throws GitAPIException {
    Git git;
    try {
      try {
        git = Git.open(dir);
      } catch (IOException e) {

        CloneCommand clone = Git.cloneRepository().setDirectory(dir).setURI(gitURI.toString());
        clone.setCredentialsProvider(provider);
        git = clone.call();
        changeBranch(git);

        return;
      }

      git.clean().call();

      FetchCommand fetch = git.fetch();
      fetch.setCredentialsProvider(provider);
      fetch.call();

      changeBranch(git);

      git.reset().setMode(ResetCommand.ResetType.HARD).call();
    } catch (TransportException e) {
      throw configError("error.wrongCredentials", e, TextComponent.of(this.dir.getName()));
    }
  }

  private void changeBranch(Git git) throws GitAPIException {
    if (repository.getBranch() != null) {
      git.checkout().setName("origin/" + repository.getBranch()).call();
    }
  }
}

package tc.oc.pgm.map.source;

import com.google.common.collect.Iterators;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.transport.ChainingCredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapMissingException;
import tc.oc.pgm.api.map.factory.MapSourceFactory;
import tc.oc.pgm.util.text.TextException;

public class GitMapSourceFactory implements MapSourceFactory {

  private final Git git;
  private final CredentialsProvider credentials;

  public GitMapSourceFactory(File file, URI uri, @Nullable String branch) {
    final String user = uri.getUserInfo();
    if (user != null) {
      final String[] parts = user.split(":", 2);
      credentials = new UsernamePasswordCredentialsProvider(parts[0], parts[1]);
    } else {
      credentials = new ChainingCredentialsProvider();
    }

    Git git;
    try (Git jgit = Git.open(file)) {
      git = jgit;
    } catch (RepositoryNotFoundException e) {
      final CloneCommand clone =
          Git.cloneRepository()
              .setDirectory(file)
              .setURI(uri.toString())
              .setCredentialsProvider(credentials)
              .setCloneAllBranches(false)
              .setCloneSubmodules(true);

      if (branch != null) {
        clone.setBranch("refs/heads/" + branch);
        clone.setBranchesToClone(Collections.singleton("refs/heads/" + branch));
      }

      PGM.get()
          .getGameLogger()
          .log(
              Level.INFO,
              String.format(
                  "Cloning %s%s on %s branch... (this may take a while)",
                  uri.getHost(), uri.getPath(), (branch == null ? "the default" : branch)));

      try {
        git = clone.call();
      } catch (InvalidRemoteException e1) {
        throw TextException.invalidFormat(uri.toString(), URI.class, e1);
      } catch (GitAPIException e2) {
        throw TextException.unknown(e2);
      }
    } catch (IOException e) {
      throw TextException.unknown(e);
    }
    this.git = git;
  }

  @Override
  public Iterator<? extends MapSource> loadNewSources() throws MapMissingException {
    try {
      git.pull()
          .setCredentialsProvider(credentials)
          .setFastForward(MergeCommand.FastForwardMode.FF)
          .call();
    } catch (GitAPIException e) {
      throw new MapMissingException(
          git.getRepository().getDirectory().getPath(), e.getMessage(), e.getCause());
    }

    return Iterators.emptyIterator();
  }

  @Override
  public void reset() {
    // No-op
  }
}

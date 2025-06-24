package tc.oc.pgm.map.source;

import static tc.oc.pgm.util.text.TextException.invalidFormat;
import static tc.oc.pgm.util.text.TextException.unknown;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.transport.ChainingCredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapException;
import tc.oc.pgm.api.map.exception.MapMissingException;

public class GitMapSourceFactory extends PathMapSourceFactory {

  private final CredentialsProvider credentials;
  private final Git git;

  public GitMapSourceFactory(
      Path base, @Nullable List<Path> children, URI uri, @Nullable String branch) {
    super(new MapRoot(base), children);

    final String user = uri.getUserInfo();
    if (user != null) {
      final String[] parts = user.split(":", 2);
      credentials = new UsernamePasswordCredentialsProvider(parts[0], parts[1]);
    } else {
      credentials = new ChainingCredentialsProvider();
    }

    this.git = openRepo(uri, branch);

    this.paths =
        new MapRoot(
            base,
            uri.getHost(),
            uri.getPath().replaceFirst("/", ""),
            createBaseUri(uri),
            uri.getUserInfo() != null);
  }

  @Override
  public Stream<MapSource> loadNewSources(Consumer<MapException> exceptionHandler) {
    try {
      git.pull()
          .setCredentialsProvider(credentials)
          .setFastForward(MergeCommand.FastForwardMode.FF)
          .call();
    } catch (GitAPIException e) {
      exceptionHandler.accept(
          new MapMissingException(
              git.getRepository().getDirectory().getPath(), e.getMessage(), e.getCause()));
    }

    return super.loadNewSources(exceptionHandler);
  }

  private Git openRepo(URI uri, @Nullable String branch) {
    try (Git jgit = Git.open(paths.getBase().toFile())) {
      return jgit;
    } catch (RepositoryNotFoundException e) {
      return cloneRepo(uri, branch);
    } catch (IOException e) {
      throw unknown(e);
    }
  }

  private Git cloneRepo(URI uri, @Nullable String branch) {
    Git git;
    final CloneCommand clone =
        Git.cloneRepository()
            .setDirectory(paths.getBase().toFile())
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
      throw invalidFormat(uri.toString(), URI.class, e1);
    } catch (GitAPIException e2) {
      throw unknown(e2);
    }
    return git;
  }

  private String createBaseUri(URI uri) {
    try {
      String path = uri.getPath();
      path += (path.endsWith("/") ? "" : "/") + "blob/" + git.getRepository().getBranch() + "/";

      // IMPORTANT: ensure user info is stripped
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path, null, null)
          .toString();
    } catch (URISyntaxException | IOException e) {
      PGM.get()
          .getGameLogger()
          .log(
              Level.WARNING,
              "Failed to create url for repository " + uri.getHost() + "/" + uri.getPath(),
              e);
      return null;
    }
  }
}

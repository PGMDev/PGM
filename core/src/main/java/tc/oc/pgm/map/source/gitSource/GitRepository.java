package tc.oc.pgm.map.source.gitSource;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import org.eclipse.jgit.transport.URIish;

public class GitRepository {
  private final URIish uri;
  private final String branch;
  private final List<String> folders;
  private final File dir;

  public GitRepository(URIish uri, String branch, List<String> folders) {
    this.uri = uri;
    this.branch = branch;
    this.folders = folders;

    String url = uri.getHost() + uri.getPath();

    dir = new File("./maps/" + url.replaceAll("[/._]", "-"));
    dir.mkdir();
  }

  public GitRepository(String uri) throws URISyntaxException {
    this(new URIish(uri), null, null);
  }

  public URIish getUri() {
    return uri;
  }

  public String getBranch() {
    return branch;
  }

  public List<String> getFolders() {
    return folders;
  }

  public File getDir() {
    return dir;
  }
}

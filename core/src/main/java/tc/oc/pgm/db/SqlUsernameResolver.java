package tc.oc.pgm.db;

import com.google.common.collect.Lists;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection;
import tc.oc.pgm.util.usernames.AbstractBatchingUsernameResolver;

public class SqlUsernameResolver extends AbstractBatchingUsernameResolver {
  private static final int BATCH_SIZE = 500;
  private final SQLDatastore datastore;

  public SqlUsernameResolver(SQLDatastore datastore) {
    this.datastore = datastore;
  }

  protected void process(UUID uuid, CompletableFuture<UsernameResponse> future) {
    datastore.submitQuery(new SingleSelect(uuid, future));
  }

  @Override
  protected void process(List<UUID> uuids) {
    List<List<UUID>> partitions = Lists.partition(uuids, BATCH_SIZE);
    CompletableFuture<?>[] futures = new CompletableFuture[partitions.size()];
    for (int i = 0; i < partitions.size(); i++) {
      futures[i] = datastore.submitQuery(new BatchSelect(partitions.get(i), this::getFuture));
    }
    CompletableFuture.allOf(futures).join();
  }

  private static class SingleSelect implements ThreadSafeConnection.Query {
    private final UUID uuid;
    private final CompletableFuture<UsernameResponse> future;

    public SingleSelect(UUID uuid, CompletableFuture<UsernameResponse> future) {
      this.uuid = uuid;
      this.future = future;
    }

    @Override
    public String getFormat() {
      return "SELECT name, expires FROM usernames WHERE id = ? LIMIT 1";
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, uuid.toString());

      try (final ResultSet result = statement.executeQuery()) {
        future.complete(
            !result.next()
                ? UsernameResponse.empty()
                : UsernameResponse.of(
                    result.getString(1),
                    null,
                    Instant.ofEpochMilli(result.getLong(2)),
                    SqlUsernameResolver.class));
      }
    }
  }

  private static class BatchSelect implements ThreadSafeConnection.Query {
    private final List<UUID> uuids;
    private final Function<UUID, CompletableFuture<UsernameResponse>> futures;

    public BatchSelect(
        List<UUID> uuids, Function<UUID, CompletableFuture<UsernameResponse>> futures) {
      this.uuids = uuids;
      this.futures = futures;
    }

    @Override
    public String getFormat() {
      return "SELECT id, name, expires FROM usernames WHERE id IN ("
          + Stream.generate(() -> "?").limit(uuids.size()).collect(Collectors.joining(","))
          + ")";
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      for (int i = 0; i < uuids.size(); i++) {
        statement.setString(i + 1, uuids.get(i).toString());
      }

      try (final ResultSet result = statement.executeQuery()) {
        Set<UUID> leftover = new HashSet<>(uuids);
        while (result.next()) {
          UUID uuid = UUID.fromString(result.getString(1));
          leftover.remove(uuid);

          futures
              .apply(uuid)
              .complete(
                  UsernameResponse.of(
                      result.getString(2),
                      null,
                      Instant.ofEpochMilli(result.getLong(3)),
                      SqlUsernameResolver.class));
        }

        leftover.forEach(uuid -> futures.apply(uuid).complete(UsernameResponse.empty()));
      }
    }
  }
}

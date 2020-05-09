package tc.oc.pgm.util.concurrent;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** A concurrent, thread-safe {@link Connection}. */
public class ThreadSafeConnection implements Closeable {

  private static final long MAX_TIMEOUT_MS = Duration.ofSeconds(10).toMillis();

  private final Supplier<Connection> connectionSupplier;
  private final BlockingQueue<Connection> connectionQueue;
  private final ExecutorService executorService;

  public ThreadSafeConnection(Supplier<Connection> connectionSupplier, int maxConnections)
      throws SQLException {
    this.connectionSupplier = connectionSupplier;
    this.connectionQueue = new LinkedBlockingQueue<>(maxConnections);
    this.executorService = Executors.newWorkStealingPool(maxConnections);

    releaseConnection(newConnection());
  }

  /** A wrapper around a {@link PreparedStatement} to execute queries. */
  @FunctionalInterface
  public interface Query {

    /**
     * Gets the query format.
     *
     * <p>e.g. "SELECT id FROM matches WHERE players > ? AND date = ?"
     *
     * @return The query format.
     */
    String getFormat();

    /**
     * Executes a query.
     *
     * @param statement An empty statement.
     * @throws SQLException If the query fails.
     */
    default void query(PreparedStatement statement) throws SQLException {
      statement.execute();
    }
  }

  /**
   * Submits a query.
   *
   * @see Query
   * @param query A query.
   * @return A future when the query is complete.
   */
  public Future<?> submitQuery(Query query) {
    return executorService.submit(
        () -> {
          try {
            final Connection connection = acquireConnection();

            try (final PreparedStatement statement =
                connection.prepareStatement(query.getFormat())) {
              query.query(statement);
            }

            releaseConnection(connection);
          } catch (SQLException e) {
            e.printStackTrace();
          }
        });
  }

  /**
   * Releases an existing connection.
   *
   * @param connection An existing connection.
   * @throws SQLException If the connection is invalid.
   */
  private void releaseConnection(Connection connection) throws SQLException {
    try {
      connectionQueue.offer(connection, MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      connection.close();
    }
  }

  /**
   * Blocks until an existing connection is available.
   *
   * @return An existing connection.
   * @throws SQLException If the connection is invalid.
   */
  private Connection acquireConnection() throws SQLException {
    Connection connection;

    try {
      connection = connectionQueue.poll(MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new SQLTimeoutException();
    }

    if (connection == null) releaseConnection(connection = newConnection());
    if (connection.isClosed()) throw new SQLTimeoutException();

    return connection;
  }

  /**
   * Establishes a new connection.
   *
   * @return A new connection.
   * @throws SQLException If the connection is invalid.
   */
  private Connection newConnection() throws SQLException {
    final Connection connection = connectionSupplier.get();

    connection.setAutoCommit(true);
    try {
      connection.setNetworkTimeout(executorService, (int) MAX_TIMEOUT_MS);
    } catch (AbstractMethodError e) {
      // No-op, really old drivers do not support timeouts
    }

    return connection;
  }

  @Override
  public void close() {
    executorService.shutdown();

    List<Connection> connections = new LinkedList<>();
    connectionQueue.drainTo(connections);

    try {
      executorService.awaitTermination(MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }

    for (Connection connection : connections) {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}

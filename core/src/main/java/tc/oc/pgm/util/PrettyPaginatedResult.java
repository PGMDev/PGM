package tc.oc.pgm.util;

import app.ashcon.intake.CommandException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.text.TextComponent;
import tc.oc.pgm.util.chat.Audience;

/**
 * Class used to display a paginated list of items that are formatted
 *
 * <p>See {@link PrettyPaginatedComponentResults} for more interactive menus
 *
 * @param <T> Type of item to format
 */
public abstract class PrettyPaginatedResult<T> {

  private String header;
  private int resultsPerPage;

  /**
   * Constructor
   *
   * @param header list header
   * @param resultsPerPage results per page
   */
  public PrettyPaginatedResult(String header, int resultsPerPage) {
    this.header = header;
    this.resultsPerPage = resultsPerPage;
  }

  /**
   * Constructor
   *
   * @param header list header
   */
  public PrettyPaginatedResult(String header) {
    this(header, 6);
  }

  /**
   * Formats the lists' header
   *
   * @param header to be formatted
   * @param currentPage being displayed
   * @param pages total pages
   * @return Formatted header
   */
  public String formatHeader(String header, int currentPage, int pages) {
    return header;
  }

  /**
   * Formats a the type data into a string
   *
   * @param data to format
   * @param index if the data
   * @return Formatted item
   */
  public abstract String format(T data, int index);

  /**
   * Format sent to the player if no data is provided
   *
   * @return Formatted message
   * @throws CommandException default implementation exception
   */
  public String formatEmpty() throws CommandException {
    throw new CommandException("No results match!");
  }

  /**
   * Displays a list of items based on the page to an audience
   *
   * @param audience to display data to
   * @param data to display
   * @param page where the data is located
   * @throws CommandException no match exceptions
   */
  public void display(Audience audience, Collection<? extends T> data, int page)
      throws CommandException {
    display(audience, new ArrayList<>(data), page);
  }

  /**
   * Displays a list of items based on the page to an audience
   *
   * @param audience to display data to
   * @param data to display
   * @param page where the data is located
   * @throws CommandException no match exceptions
   */
  public void display(Audience audience, List<? extends T> data, int page) throws CommandException {
    if (data.size() == 0) {
      audience.sendMessage(TextComponent.of(formatEmpty()));
      return;
    }

    int maxPages = data.size() / this.resultsPerPage + 1;

    if (data.size() % this.resultsPerPage == 0) {
      maxPages--;
    }

    if (page <= 0 || page > maxPages)
      throw new CommandException("Unknown page selected! " + maxPages + " total pages.");

    StringBuilder message = new StringBuilder(header + "\n");
    for (int i = resultsPerPage * (page - 1);
        i < this.resultsPerPage * page && i < data.size();
        i++) {
      message.append(format(data.get(i), i));
      if (i != (data.size() - 1)) message.append("\n");
    }
    audience.sendMessage(TextComponent.of(message.toString()));
  }
}

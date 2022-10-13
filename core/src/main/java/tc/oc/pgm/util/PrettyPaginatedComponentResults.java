package tc.oc.pgm.util;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.util.text.TextException;

public abstract class PrettyPaginatedComponentResults<T> {

  private Component header;
  private int resultsPerPage;

  /**
   * Constructor
   *
   * @param header list header
   * @param resultsPerPage results per page
   */
  public PrettyPaginatedComponentResults(Component header, int resultsPerPage) {
    this.header = header;
    this.resultsPerPage = resultsPerPage;
  }

  /**
   * Constructor
   *
   * @param header list header
   */
  public PrettyPaginatedComponentResults(Component header) {
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
  public Component formatHeader(Component header, int currentPage, int pages) {
    return header;
  }

  /**
   * Formats a the type data into a string
   *
   * @param data to format
   * @param index if the data
   * @return Formatted item
   */
  public abstract Component format(T data, int index);

  /**
   * Format sent to the player if no data is provided
   *
   * @return Formatted message
   * @throws TextException default implementation exception
   */
  public Component formatEmpty() throws TextException {
    throw exception("menu.page.empty");
  }

  /**
   * Displays a list of items based on the page to an audience
   *
   * @param audience to display data to
   * @param data to display
   * @param page where the data is located
   * @throws TextException no match exceptions
   */
  public void display(Audience audience, Collection<? extends T> data, int page)
      throws TextException {
    display(audience, new ArrayList<>(data), page);
  }

  /**
   * Displays a list of items based on the page to an audience
   *
   * @param audience to display data to
   * @param data to display
   * @param page where the data is located
   * @throws TextException no match exceptions
   */
  public void display(Audience audience, List<? extends T> data, int page) throws TextException {
    if (data.size() == 0) {
      audience.sendMessage(formatEmpty());
      return;
    }

    int maxPages = data.size() / this.resultsPerPage + 1;

    if (data.size() % this.resultsPerPage == 0) {
      maxPages--;
    }

    if (page <= 0 || page > maxPages)
      throw exception("command.invalidPage", text(page), text(maxPages));

    audience.sendMessage(header);
    for (int i = resultsPerPage * (page - 1);
        i < this.resultsPerPage * page && i < data.size();
        i++) {
      audience.sendMessage(format(data.get(i), i));
    }
  }
}

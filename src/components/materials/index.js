import React, { useRef, useEffect } from "react";

import classnames from "classnames";
import styles from "./styles.module.css";

let parsedRowData = null;

const doMaterialSearch = (searchField, resultCount) => {
  let targetTable = document.getElementById("dataTable");

  // Index the table.
  if (!parsedRowData) {
    let rowData = targetTable.rows;
    parsedRowData = new Array();
    for (let rowIndex = 0; rowIndex < rowData.length; rowIndex++) {
      let tableRow = rowData.item(rowIndex);
      // Not a data row, ignore.
      if (tableRow.cells.length != 4) {
        continue;
      }

      // Get and nicely format the search-able terms in the table.
      // We add the text to an empty string so that we don't do indexOf against null later on in the code.
      let materialUID =
        "" +
        tableRow.cells
          .item(0)
          .textContent.trim()
          .replace(/\s\s+/g, " ")
          .toLocaleLowerCase();
      let materialName =
        "" +
        tableRow.cells
          .item(2)
          .textContent.trim()
          .replace(/\s\s+/g, " ")
          .replace(/(_)/gm, " ")
          .toLocaleLowerCase();

      let materialTags =
        "" +
        tableRow.cells
          .item(3)
          .textContent.trim()
          .replace(/\s\s+/g, " ")
          .toLocaleLowerCase()
          .split(", ");

      parsedRowData.push([tableRow, materialUID, materialName, materialTags]);
    }
  }

  // Format the search query
  let searchText = searchField.current.value.trim().toLocaleLowerCase();
  // Find and remove all search terms that are indicated to be tags
  let tagSearchTerms = searchText.match(/~\S*/gi);
  searchText = searchText.replace(/~\S*/gi, "").trim();
  if (tagSearchTerms) {
    for (let i = 0; i < tagSearchTerms.length; i++) {
      // Remove leading ~ from all tag search terms.
      tagSearchTerms[i] = tagSearchTerms[i].slice(1);
    }
  }

  // Create the query regex pattern
  let searchKeys = searchText.split(" ");
  let searchRegexS = "^";
  for (let i = 0; i < searchKeys.length; i++) {
    searchRegexS += "(?=.*\\b" + searchKeys[i] + ")";
  }
  searchRegexS += ".+";

  let searchRegex = new RegExp(searchRegexS, "i");

  let matchingRows = new Array();

  // Loop through the tables rows and check if they match the query.
  for (let rowIndex = 0; rowIndex < parsedRowData.length; rowIndex++) {
    let rowData = parsedRowData[rowIndex];

    let materialUID = rowData[1];
    let materialName = rowData[2];
    let materialTags = rowData[3];

    let matches = false;

    // If the search term is a number only match the materials UID.
    if (!isNaN(searchText)) {
      // Only return true if the search result starts at the beginning of the UID
      // This will prevent a search query of 8 from returning 18.
      if (materialUID.search(searchText) == 0) {
        matches = true;
      }
    }

    // Otherwise match the materials name
    // If searchText is "" (nothing) returns 0 instead of -1?
    // This isn't really a problem since we want the table to show everything if there is no search term.
    // This also allows us to search just by tag without having to add another else if.
    else if (materialName.search(searchRegex) != -1) {
      matches = true;
      // If we have tags, mark any materials that don't match the tags as non-matching.
      // This will result in us only having materials that match both the query and the tags.
      if (tagSearchTerms) {
        for (let i = 0; i < tagSearchTerms.length; i++) {
          if (materialTags.indexOf(tagSearchTerms[i]) == -1) {
            matches = false;
          }
        }
      }
    }

    // Add matching row
    if (matches === true) {
      matchingRows.push(rowData);
    }
  }

  // Update the result counter.
  let count = matchingRows.length.toString();
  resultCount.current.textContent = count + " Results";
  // Create a new table body and replace the old one.
  const newTableBody = document.createElement("tbody");
  newTableBody.setAttribute("id", "dataTable");
  populateWithRows(matchingRows, newTableBody);
  targetTable.parentNode.replaceChild(newTableBody, targetTable);
  return true;
};

const populateWithRows = (matchingRows, newTableBody) => {
  let row;

  // If there are no matching rows add one indicating this.
  if (matchingRows.length < 1) {
    row = document.createElement("tr");
    let cell = document.createElement("td");
    let cellText = document.createTextNode("No materials match the query.");

    cell.appendChild(cellText);
    row.appendChild(cell);
    row.setAttribute("id", "no_items");
    cell.setAttribute("colspan", "4");
    // Insert it as the first child.
    newTableBody.insertBefore(row, newTableBody.firstChild);
  } else {
    for (let rowIndex = 0; rowIndex < matchingRows.length; rowIndex++) {
      let matchingRow = matchingRows[rowIndex];
      row = matchingRow[0];
      newTableBody.appendChild(row);
    }
  }
};

const Materials = ({ children }) => {
  const searchField = useRef();
  const resultCount = useRef();

  useEffect(() => {
    doMaterialSearch(searchField, resultCount);
  }, []);

  return (
    <>
      <form role="search">
        <div className={classnames(styles.search)}>
          <input
            ref={searchField}
            onChange={() => doMaterialSearch(searchField, resultCount)}
            placeholder="Search..."
          />{" "}
          <span ref={resultCount}></span>
        </div>
      </form>
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>UID</th>
              <th>Data</th>
              <th>Name</th>
              <th>Tags</th>
            </tr>
          </thead>
          <tbody id="dataTable">{children}</tbody>
        </table>
      </div>
    </>
  );
};

export default Materials;

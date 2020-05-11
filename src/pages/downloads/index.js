import React, { useState, useEffect } from "react";
import Layout from "@theme/Layout";

import { format } from "date-fns";

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faArrowDown,
  faAngleRight,
  faFolderOpen,
} from "@fortawesome/free-solid-svg-icons";

import classnames from "classnames";
import styles from "./styles.module.css";

export default function Downloads() {
  const [releases, setReleases] = useState([]);

  useEffect(() => {
    async function getReleases() {
      const response = await fetch(
        "https://api.github.com/repos/Electroid/PGM/releases"
      );

      return setReleases(await response.json());
    }

    getReleases();
  }, []);

  return (
    <Layout title="Downloads">
      <div className={classnames("container", styles.downloads_container)}>
        <h1 className={styles.downloads_title}>Downloads</h1>
        {releases.length === 0 ? (
          <div className={classnames("hero", styles.spinner_hero)}>
            <div className={styles.lds_ripple}>
              <div></div>
              <div></div>
            </div>
          </div>
        ) : (
          <div
            className={classnames(
              "hero",
              styles.downloads_hero,
              styles.appearing
            )}
          >
            <div className="container row">
              <div className="col">
                <div className="row">
                  <img src="/img/shield.png" className={styles.pgm_logo} />
                  <div className={styles.pgm}>
                    <h1>PGM {releases[0].name}</h1>
                    <p>
                      <label>
                        {" "}
                        {format(
                          new Date(releases[0].published_at),
                          "LLLL do, yyyy"
                        )}{" "}
                      </label>
                    </p>
                    <a href={releases[0].html_url}>
                      Changelog <FontAwesomeIcon icon={faAngleRight} />
                    </a>
                  </div>
                </div>
                <div className={styles.description}>
                  <p>
                    Minecraft multiplayer game-server suite for managing PvP
                    matches across various gamemodes
                  </p>
                </div>
              </div>
              <div className={classnames("col col--4", styles.col_border)}>
                <h2>Download PGM</h2>
                <p>
                  PvP Game Manager (PGM) is a plugin that manages running and
                  instancing multiple PvP maps across various gamemodes for
                  Minecraft multiplayer.
                </p>
                <a
                  className={classnames(
                    "button button--primary",
                    styles.download_button
                  )}
                  href={releases[0].assets[0].browser_download_url}
                >
                  Download <FontAwesomeIcon icon={faArrowDown} />
                </a>
              </div>
              <div className={classnames("col col--4", styles.col_margin_left)}>
                <h2>Download SportPaper</h2>
                <p>
                  SportPaper is a Minecraft server jar based on Paper 1.8 tuned
                  for maximum performance for high intensity PvP. It is
                  mandatory and should run along PGM.
                </p>
                <a
                  className={classnames(
                    "button button--primary",
                    styles.download_button
                  )}
                  href={releases[0].assets[1].browser_download_url}
                >
                  Download <FontAwesomeIcon icon={faArrowDown} />
                </a>
              </div>
            </div>
          </div>
        )}
        <div className={styles.others}>
          <h2>Other Resources</h2>
          <div className="row">
            <div className="col col--6">
              <div className={classnames("hero", styles.others_hero)}>
                <div className="col">
                  <div className="row">
                    <div className="col col--6">
                      <div className="row">
                        <h1 className={styles.others_icon}>
                          <FontAwesomeIcon icon={faFolderOpen} />
                        </h1>
                        <div className={styles.others_header}>
                          <h2>ResourcePile</h2>
                          <a href="https://github.com/MCResourcePile">
                            <label>GitHub</label>
                          </a>
                        </div>
                      </div>
                    </div>
                    <div className="col col--6">
                      <a
                        className={classnames(
                          "button button--primary",
                          styles.others_button
                        )}
                        href="https://mcresourcepile.github.io"
                      >
                        Visit <FontAwesomeIcon icon={faFolderOpen} />
                      </a>
                    </div>
                  </div>
                  <div className={styles.others_description}>
                    <p>
                      ResourcePile is a community project which aims to provide
                      various collections of resources, such as maps or
                      statistics, for users with backgrounds in Overcast and
                      similar networks.
                    </p>
                  </div>
                </div>
              </div>
            </div>
            <div className={classnames("col col--6", styles.others_column)}>
              <div className={classnames("hero", styles.others_hero)}>
                <div className="col">
                  <div className="row">
                    <img
                      src="https://avatars3.githubusercontent.com/u/54190830?s=200&v=4"
                      className={styles.octc_logo}
                    />
                    <div className={styles.others_header}>
                      <h2>Overcast Community</h2>
                      <a href="https://oc.tc/">Website</a>
                    </div>
                  </div>
                  <div className={styles.others_description}>
                    <p>
                      An old favorite is back! Get to play a variety of classic
                      Minecraft PvP maps and get involved with PGM development
                      by trying beta features! Join today @{" "}
                      <code>play.oc.tc</code>
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}

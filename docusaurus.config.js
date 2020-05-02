module.exports = {
  title: "PGM Documentation",
  tagline: "Documentation for PGM, the original Minecraft PvP Game Manager",
  url: "https://electroid.github.io/",
  baseUrl: "/PGM/",
  favicon: "img/favicon.png",
  organizationName: "Electroid", // Usually your GitHub org/user name.
  projectName: "PGM", // Usually your repo name.
  themeConfig: {
    navbar: {
      title: "PGM Documentation",
      logo: {
        alt: "Logo",
        src: "img/logo.png",
      },
      links: [
        {
          to: "docs/modules/main",
          activeBasePath: "docs",
          label: "Modules",
          position: "left",
        },
        {
          to: "docs/reference/inventory",
          activeBasePath: "docs",
          label: "Reference",
          position: "left",
        },
        {
          to: "docs/guides/regions",
          activeBasePath: "docs",
          label: "Guides",
          position: "left",
        },
        {
          to: "docs/doc1",
          activeBasePath: "docs",
          label: "Examples",
          position: "left",
        },
        {
          href: "https://github.com/Electroid/PGM",
          label: "GitHub",
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Docs",
          items: [
            {
              label: "Style Guide",
              to: "docs/doc1",
            },
            {
              label: "Second Doc",
              to: "docs/doc2",
            },
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "Discord",
              href: "https://discord.gg/CvJGbrV",
            },
            {
              label: "Twitter",
              href: "https://twitter.com/OvercastPGM",
            },
          ],
        },
        {
          title: "More",
          items: [
            {
              label: "GitHub",
              href: "https://github.com/Electroid/PGM",
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} The PGM project and its contributors`,
    },
  },
  presets: [
    [
      "@docusaurus/preset-classic",
      {
        docs: {
          sidebarPath: require.resolve("./sidebars.js"),
          editUrl: "https://github.com/Electroid/PGM/edit/docs",
        },
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
      },
    ],
  ],
};

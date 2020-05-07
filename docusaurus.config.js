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
          to: "docs/modules/general/main",
          activeBasePath: "docs",
          label: "Modules",
          position: "left",
        },
        {
          to: "docs/reference/items/inventory",
          activeBasePath: "docs",
          label: "Reference",
          position: "left",
        },
        {
          to: "docs/guides/xml-pointers/regions",
          activeBasePath: "docs",
          label: "Guides",
          position: "left",
        },
        {
          to: "docs/modules/general/main",
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
              to: "docs/modules/general/main",
              label: "Modules",
            },
            {
              to: "docs/reference/items/inventory",
              label: "Reference",
            },
            {
              to: "docs/guides/xml-pointers/regions",
              label: "Guides",
            },
            {
              to: "docs/modules/general/main",
              label: "Examples",
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

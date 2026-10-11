[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/BenWoodworth/FastCraft?include_prereleases)](https://github.com/BenWoodworth/FastCraft/releases)
[![Downloads](https://img.shields.io/github/downloads/BenWoodworth/FastCraft/total.svg)](https://github.com/BenWoodworth/FastCraft/releases)
[![Donate](https://img.shields.io/badge/stats-Bukkit-blue)](https://bstats.org/plugin/bukkit/FastCraft/1381)
[![Donate](https://img.shields.io/badge/donate-PayPal-yellow)](https://paypal.me/BenWoodworth)

# FastCraft

> **Platform requirement:** The new distribution targets **Paper 1.21.11** running on **Java 21**. Spigot, legacy Bukkit and older Minecraft versions are not supported.

The plugin entrypoint and its `plugin.yml` are maintained in `fastcraft-paper/`. Use `./gradlew build` for the complete build and `./gradlew runServer` for a local Paper 1.21.11 server.

**Migration status:** Some historical adapter classes are still compiled as internal dependencies of the Paper implementation. The migration to a single implementation module is in progress; see [PAPER_ONLY_MIGRATION.md](PAPER_ONLY_MIGRATION.md). Do not treat those internal sources as a compatibility guarantee.

FastCraft is a Minecraft server plugin that changes the
way players craft their items, allowing items to be crafted
in a single click. Instead of the usual 3x3 crafting grid,
players will be shown all the items
that can be crafted from the items in their inventory, and
clicking one of the items will automatically remove the
required ingredients from the players inventory, and give
the newly crafted item.

<p align="center">
    <img src="assets/fastcraft-ui.png" alt="FastCraft GUI" />
</p>

All documentation can be found on the [FastCraft wiki](https://github.com/BenWoodworth/FastCraft/wiki).

# Paper 1.21.11-only migration — issue #3

## Target

Only `:fastcraft-paper` is a deployable plugin. It requires **Paper 1.21.11** and **Java 21**, with one `plugin.yml` pointing at `net.benwoodworth.fastcraft.paper.FastCraftPaper`. `:fastcraft-core` and `:fastcraft-api` remain platform-neutral.

## Implemented in the issue #3 continuation

- Consolidated the Dagger `@Component` and `@Module` into `fastcraft-paper`. `FastCraftPaper` now directly extends `JavaPlugin`, starts/stops the FastCraft engine and refuses unsupported server names and versions. The previous `BukkitFastCraft` entrypoint has been removed.
- Moved the **Paper 1.21.11 crafting recipe factories, synthetic inventory view and JUnit/MockBukkit test** into `fastcraft-paper`; removed the old `paper-1.21.11` subproject.
- Copied the **16 active implementations** from `bukkit-1.8`, `bukkit-1.9`, `bukkit-1.12`, `bukkit-1.13` and `bukkit-1.15` into the Paper module, preserving their package and inheritance compatibility during the transition. Removed all five Gradle subprojects and four unused historical implementations.
- Removed 19 obsolete version-specific configuration and item-order files, plus three old offline material-generator tools. These resources were looked up by the detected `major.minor` version, so the runtime restricted to `1.21.11` did not read historical 1.7–1.16 resources.
- The remaining Bukkit bridge module now compiles against Paper API 1.21.11, not Spigot 1.8. Removed its obsolete `processResources` expansion of the former plugin descriptor.
- The runtime retains exactly one `plugin.yml`, located at `fastcraft-paper/src/main/resources/plugin.yml`.

## Remaining technical debt before closing issue #3

Three legacy adapter modules remain: `:fastcraft-bukkit:bukkit-1.7`, `:fastcraft-bukkit:bukkit-platform` and `:fastcraft-bukkit:fastcraft-api-bukkit`, plus the `:fastcraft-bukkit` aggregator. These are **not supported server targets** but are still linked internally.

1. Rewrite legacy 1.7 command, player, GUI, recipe, item, inventory, sound and configuration implementations using the Paper 1.21.11 API. Eliminate old API constants (`Material.WORKBENCH`, pre-flattening material data, legacy crafting inventory APIs) and hidden dependencies on Spigot 1.7.
2. Move the shared Bukkit interfaces, API adapter and rewritten implementations into `fastcraft-paper`; remove the remaining four modules from `settings.gradle` and the checked-in Spigot 1.7 API JAR.
3. Flatten the remaining type names and inherited implementations (currently suffixed `_1_7`, `_1_8`, etc.). Preserve crafting event semantics, persisted config/API compatibility, inventory transactions and recipe behavior.
4. Remove the Spigot/Sonatype compatibility repositories. Refine the root Shadow dependency allowlist to package only Paper implementation, platform-neutral core/API and required embedded dependencies.
5. Verify unit and integration behavior; add regression coverage for shaped/shapeless and special recipes, cancelled prepare events, shift-click, and full inventory.

## Validation

```bash
./gradlew --version
./gradlew :fastcraft-paper:test
./gradlew clean build
./gradlew verifyShadedInjectionApi
./gradlew runServer
```

Then test `/fastcraft`, `/fastcraft craft`, `/fastcraft set enabled toggle` and `/fastcraft reload`; validate synthetic inventory slots, shaped/shapeless/custom recipes, cancelled prepare events and inventory transaction edge cases.

**Build/test status:** No local Gradle distribution or accessible dependency cache is available in the assistant environment. GitHub Actions is expected to run after the continuation PR is opened; any failures must be fixed before considering the PR ready. Do not merge or close issue #3 until source consolidation and validations are complete.

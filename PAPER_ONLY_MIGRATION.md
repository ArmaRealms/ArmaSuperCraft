# Paper 1.21.11-only migration

## Target architecture

- `:fastcraft-paper`: the only supported server entrypoint, compiled against Paper API 1.21.11 and Java 21.
- `:fastcraft-core` and `:fastcraft-api`: platform-neutral business logic and public API (internal project modules, not compatibility promises).
- One shaded JAR containing the FastCraft plugin with a `plugin.yml` declaring `api-version: 1.21.11` and main `net.benwoodworth.fastcraft.paper.FastCraftPaper`.
- No distribution for Spigot or any Minecraft release other than 1.21.11.

## Already changed

- Added `fastcraft-paper` with a Paper-only `JavaPlugin` entrypoint, Paper API dependency and a descriptor regression test.
- Moved `plugin.yml` ownership away from the legacy Bukkit aggregator.
- The Dagger module no longer branches between implementations based on historical Bukkit versions. It binds the newest available implementations and Paper 1.21.11 synthetic crafting view / recipe factories.
- Removed the unused `bukkit-1.14` and `paper-1.15` Gradle modules and their test code.
- Reused FastCraft's existing runtime, GUI, command handling and shaded dependencies while reorganizing the plugin.

## Remaining blocking refactor

The current Paper implementation is **not independent of legacy adapter modules**. Removing every `:fastcraft-bukkit:bukkit-*` module today would fail compilation due to inheritance and implementation references. The following must be completed before claiming that the old source modules have been fully removed:

1. Move the server-neutral Bukkit interfaces from `:fastcraft-bukkit:bukkit-platform` and `:fastcraft-bukkit:fastcraft-api-bukkit` into `:fastcraft-paper` (or eliminate adapters when the Paper API suffices).
2. Replace legacy 1.7 implementations still used by the modern implementations (commands, player/inventory operations, GUI, configuration, permissions, sound, text and item conversions) with Paper-only classes.
3. Flatten the 1.8/1.9/1.12/1.13/1.15 inheritance chain for recipe preparation, crafting views, ingredients, item operations, player events and localization. Keep matrix/result semantics of the Paper 1.21.11 implementation.
4. Move the current `:fastcraft-bukkit:paper-1.21.11` implementation and tests into `:fastcraft-paper`, then remove the remaining old version modules from `settings.gradle`.
5. Remove `:fastcraft-bukkit` itself, historical Spigot Maven repositories and any bundled pre-1.21 API binary. Make the root Shadow package only `fastcraft-paper` + platform-neutral modules; update the injector and verify all required relocated classes remain.
6. Clean up version suffixes and obsolete Bukkit API probes from implementation names and docs. Preserve config migration and existing user data.

## Validation / acceptance

These checks have **not been run for this new refactor branch** and are required before merging:

```bash
./gradlew --version
./gradlew :fastcraft-paper:test
./gradlew clean build
./gradlew verifyShadedInjectionApi
./gradlew runServer
```

Test at least `/fastcraft`, `/fastcraft craft`, `/fastcraft set enabled toggle`, `/fastcraft reload`, shaped/shapeless recipes, custom recipes, inventory full/partial transactions, and cancellation handling. Inspect shaded JAR for a single descriptor and absence of Spigot API dependencies. Do not disable tests or treat compilation alone as proof of gameplay compatibility.

**State:** Paper 1.21.11 is the only advertised and guarded runtime. Old adapters remain internally until the source code consolidation is complete. The PR must remain **draft** until those dependencies are removed and the build/in-game validations pass.

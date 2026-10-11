# Paper 1.21.11-only migration (issue #3)

## Architecture

- `:fastcraft-paper`: **sole Minecraft platform module and plugin entrypoint**, compiled against Paper API 1.21.11 with Java 21.
- `:fastcraft-core`: common FastCraft logic (commands, GUI, crafting engine, persistence).
- `:fastcraft-api`: public FastCraft interfaces.
- Root Gradle packages **one shaded plugin JAR**, with exactly one `plugin.yml` declaring `api-version: 1.21.11` and `net.benwoodworth.fastcraft.paper.FastCraftPaper`.
- Bukkit/Spigot 1.7–1.15 are **not supported** as server platforms; use only Paper 1.21.11.

## Completed in continuation PR #5

- Replaced the legacy `BukkitFastCraft` entrypoint with a direct `FastCraftPaper : JavaPlugin` lifecycle and dedicated Dagger `FastCraftPaperModule`. Server name and Minecraft version are checked before enabling.
- Moved Paper 1.21.11 synthetic crafting inventory, recipe factories and test into `fastcraft-paper`.
- Consolidated 16 classes from the versioned 1.8/1.9/1.12/1.13/1.15 adapters, preserving the existing behavior and inheritance while removing those Gradle submodules.
- Moved the command, player, inventory, sound, config, GUI, text and server implementations formerly in the 1.7 adapter to `fastcraft-paper`.
- Refactored modern item and ItemStack factories to avoid inheriting pre-flattening `MaterialData` logic or using removed `Material.WORKBENCH` / `Material.STAINED_GLASS_PANE` constants. Deleted unused `LegacyMaterialInfo` provider and `MaterialData` factory/operation adapters; added a MockBukkit-backed test of modern material resolution.
- Copied shared Bukkit API bridge interfaces and existing API adapters to the sole Paper module. Replaced the entire 1.7/1.12/1.13/1.15 recipe and recipe-provider inheritance chains with single Paper-only implementations.
- Removed **all** `fastcraft-bukkit` Gradle modules, old `bukkit-platform` and `fastcraft-api-bukkit` modules, the checked-in Spigot 1.7 API JAR, obsolete resources and tooling.
- Eliminated all Gradle references to old Spigot API versions and set all three remaining source modules to Java 21.

## Remaining verification and cleanup

- [ ] Verify `./gradlew :fastcraft-paper:test` and `./gradlew clean build`. The assistant environment has no accessible Gradle distribution/cache to run these tasks.
- [ ] Verify `./gradlew verifyShadedInjectionApi` and inspect the shaded JAR for exactly one plugin descriptor, no `org/bukkit` server API classes, and both relocated `javax.inject.Provider` / `jakarta.inject.Provider`.
- [ ] Launch `./gradlew runServer` (Java 21 / Paper 1.21.11) and ensure Dagger initialization succeeds.
- [ ] Exercise `/fastcraft`, `/fastcraft craft`, `/fastcraft set enabled toggle`, and `/fastcraft reload` as player and console.
- [ ] Test shaped/shapeless/custom recipes, synthetic result + matrix slots, shift-click, full inventories and cancellation of crafting events.
- [x] Consolidated the versioned crafting recipes/providers into single Paper implementations. Removed obsolete `_1_7`/`_1_13` etc. class names, unused BukkitVersion parser/tests, duplicate player inventory event listeners and unsupported old material/sound/tooltip identifiers.
- [x] Added JUnit/MockBukkit coverage for shaped/shapeless/complex recipe handling and modern item factories.
- [x] Strengthened `verifyShadedInjectionApi` to reject duplicate/misconfigured plugin metadata and any bundled `org/bukkit/` or `io/papermc/paper/` classes.
- [ ] Review remaining deprecated API usages not required for platform compatibility (e.g., legacy display-name helpers) after build/runtime validation; preserve persisted config formats.

```bash
./gradlew --version
./gradlew :fastcraft-paper:test --stacktrace
./gradlew clean build --stacktrace
./gradlew verifyShadedInjectionApi
./gradlew runServer
```

**Status:** Source and Gradle consolidation implemented, but runtime and tests **not yet verified**. PR #5 remains draft; do not merge or close issue #3 until validation and remaining cleanup are complete.

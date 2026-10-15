# Paper-only migration (issues #3 and #7)

## Runtime and compatibility

- `:fastcraft-paper` is the sole server module and entry point, targeting Paper 1.21.11 / Java 21. Compatible forks are not rejected by their name or version string.
- `:fastcraft-core` retains crafting, GUI orchestration, commands and persistence. `:fastcraft-api` preserves the public preference interfaces and the existing `player-settings.dat` format.
- The root build produces one shaded JAR with `paper-plugin.yml`, `FastCraftPaper` and both relocated injection APIs. Bukkit/Paper and Adventure are supplied by the server and must not be bundled.
- Messages, titles, lore and item names pass native Adventure `Component` values directly. The MiniMessage helper only resolves typed literal/component placeholders and the default italic decoration; there is no text/color wrapper hierarchy or legacy serializer.
- Server messages use the configured JSON language with English fallback. Vanilla item names remain translatable for the client; custom names/lore retain their components. No Mojang language assets are downloaded by the plugin.
- Language files accept MiniMessage only, as explicitly requested in issue #7 comments. Existing files are preserved; obsolete formatting logs an error and uses fallback. See [README](README.md) for editing and reload behavior.
- Brigadier registers `/supercraft` with the non-overriding `/craft` alias, plus administrative `/fastcraft` and `/fc`. `/fastcraft:supercraft` is the unambiguous personal fallback. See the README for permissions and collisions.
- Empty Bukkit marker interfaces and obsolete text factories are removed. Remaining platform interfaces carry crafting operations, native Bukkit access or test seams; Dagger providers retain the initialization boundaries of the crafting graph.

## Verification

```bash
./gradlew :fastcraft-paper:test --stacktrace
./gradlew clean build --stacktrace
./gradlew verifyShadedInjectionApi
./gradlew runServer
```

Unit/MockBukkit tests cover commands, persisted preferences, configuration reload, translations, safe placeholders, GUI components, recipe adapters and deferred workbench switching. Server startup and console smoke tests do not prove client rendering or the full crafting/dupe matrix. Record executed results in the PR; follow the README's client/server scenarios before closing issues #3 and #7.

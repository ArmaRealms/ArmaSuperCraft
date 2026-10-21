# Changelog

This project follows [Semantic Versioning 2.0.0](https://semver.org/).

## [4.0.0] - Unreleased

First release of the ArmaSuperCraft Java 25 line. Create the `4.0.0` Git tag
and publish the GitHub Release only after CI, JitPack publication and Paper
integration tests pass.

### Added

- Public delegated crafting provider API for player-specific recipes, including
  exact ingredient quantities, full item metadata and delegated atomic commits.
- Optional recipe-provider registration via Paper `ServicesManager`; claimed
  recipes are kept away from FastCraft's synthetic vanilla crafting pipeline.
- Multi-module Maven publication for `fastcraft-api`, `fastcraft-core`, and
  `fastcraft-paper` through JitPack.

### Changed

- Java **25** minimum runtime and build toolchain, including Kotlin JVM target,
  GitHub Actions and JitPack.
- Paper 1.21.11 remains the supported API baseline.
- User commands `/craft`, `/craft alternar`, `/craft on` and `/craft off`
  use `armamc.supercraft.craft.use`; administrative commands use
  `armamc.supercraft.admin`.
- The executable plugin still ships as a shaded JAR; Maven API modules remain
  unshaded for other plugins to compile against.

### Breaking changes / migration

- **Java 21 is no longer supported.** Server JVM must be Java 25 or later.
- Legacy `fastcraft.*` permission nodes are no longer recognized. Grant the
  two new `armamc.supercraft.*` nodes in LuckPerms before deployment.
- Delegated crafting recipes must be implemented by a compatible provider.
  The provider owns all ingredient consumption and result delivery; vanilla
  recipe events must not be used as a substitute for its transaction contract.
- Keep `mcmmoaddon:*` excluded from FastCraft recipe discovery until both the
  published API and mcMMOAddon provider pass integration tests together.

### Release verification

Run `./gradlew clean build verifyShadedInjectionApi publishToMavenLocal` with
JDK 25; inspect all three generated Maven publications and the shaded plugin,
then test in Paper 1.21.11 with the actual addons. Release status is not implied
by this changelog entry.

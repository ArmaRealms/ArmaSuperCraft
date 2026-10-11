[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/BenWoodworth/FastCraft?include_prereleases)](https://github.com/BenWoodworth/FastCraft/releases)
[![Downloads](https://img.shields.io/github/downloads/BenWoodworth/FastCraft/total.svg)](https://github.com/BenWoodworth/FastCraft/releases)
[![Donate](https://img.shields.io/badge/stats-Bukkit-blue)](https://bstats.org/plugin/bukkit/FastCraft/1381)
[![Donate](https://img.shields.io/badge/donate-PayPal-yellow)](https://paypal.me/BenWoodworth)

# FastCraft

> **Platform requirement:** Built against **Paper API 1.21.11** on **Java 21**. Paper-compatible forks such as Leaf are supported without checking the server brand. Legacy Bukkit/Spigot versions are not targeted.

The plugin entrypoint and its `plugin.yml` are maintained in `fastcraft-paper/`. Use `./gradlew build` for the complete build and `./gradlew runServer` for a local Paper 1.21.11 server.

**Migration status:** Only `fastcraft-api`, `fastcraft-core` and `fastcraft-paper` remain as Gradle modules; the former Bukkit/Spigot modules and the bundled Spigot 1.7 API JAR are removed. Legacy version-suffixed implementation class names have also been replaced with Paper-specific names. Build/test and Paper runtime validation of the consolidation are still required; see [PAPER_ONLY_MIGRATION.md](PAPER_ONLY_MIGRATION.md).

## Idioma / Localization

O FastCraft utiliza **português brasileiro (`pt_BR`) por padrão**. No primeiro início, cria `plugins/FastCraft/config.yml` com a opção:

```yaml
language: pt_BR
```

As traduções incluídas no JAR são exportadas para `plugins/FastCraft/lang/*.json` **somente quando o arquivo ainda não existe**. Edite `lang/pt_BR.json` para personalizar mensagens, ou selecione outro arquivo pela opção `language` (por exemplo `en`, `es` ou `pt-BR.json`). Também é possível adicionar JSONs próprios à pasta. Execute `/fastcraft reload` para aplicar alterações, sem reiniciar o servidor.

A opção `language` é global para as mensagens da interface e dos comandos do FastCraft; não é determinada pelo idioma do cliente. Chaves ausentes ou com valor `null` usam o texto de `en.json`. Arquivos inválidos são registrados no console e também usam o fallback inglês. Nomes de arquivos não podem conter caminhos relativos ou absolutos.

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

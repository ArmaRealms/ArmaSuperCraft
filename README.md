[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/BenWoodworth/FastCraft?include_prereleases)](https://github.com/BenWoodworth/FastCraft/releases)
[![Downloads](https://img.shields.io/github/downloads/BenWoodworth/FastCraft/total.svg)](https://github.com/BenWoodworth/FastCraft/releases)
[![Donate](https://img.shields.io/badge/stats-Bukkit-blue)](https://bstats.org/plugin/bukkit/FastCraft/1381)
[![Donate](https://img.shields.io/badge/donate-PayPal-yellow)](https://paypal.me/BenWoodworth)

# FastCraft

> **Platform requirement:** The new distribution targets **Paper 1.21.11** running on **Java 21**. Spigot, legacy Bukkit and older Minecraft versions are not supported.

The plugin entrypoint and its `plugin.yml` are maintained in `fastcraft-paper/`. Use `./gradlew build` for the complete build and `./gradlew runServer` for a local Paper 1.21.11 server.

**Migration status:** Only `fastcraft-api`, `fastcraft-core` and `fastcraft-paper` remain as Gradle modules; the former Bukkit/Spigot modules and the bundled Spigot 1.7 API JAR are removed. Legacy version-suffixed implementation class names have also been replaced with Paper-specific names. Build/test and Paper runtime validation of the consolidation are still required; see [PAPER_ONLY_MIGRATION.md](PAPER_ONLY_MIGRATION.md).

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


## Comando nativo Paper (migração #7)

O comando `/craft` usa o registro Brigadier do Paper com nós literais e sugestões
gerenciadas pelo próprio cliente:

- `/craft`: abre a interface FastCraft.
- `/craft alternar`: alterna a preferência pessoal.
- `/craft on`: ativa a preferência.
- `/craft off`: desativa a preferência (forma canônica; não existe `/craft offf`).

As permissões atuais `fastcraft.command.craft.fastcraft` e
`fastcraft.command.set.enabled` são reutilizadas. O comando legado `/fastcraft`
(`/fc`) permanece disponível para os recursos de administração, grid e reload
durante a migração. Se outro plugin também registrar `/craft`, use o comando
namespaced para resolver a colisão e revise a prioridade dos plugins instalados.


## Mensagens MiniMessage e idiomas (etapa 3, #7)

O idioma global do plugin é configurado em `plugins/FastCraft/config.yml` com
`language: pt_BR` como padrão. Os arquivos `lang/*.json` do JAR são copiados
para `plugins/FastCraft/lang/` somente quando ausentes, preservando suas edições.
Chaves ausentes usam a versão do `en.json`. Modificações são reaplicadas com
`/fastcraft reload`.

Mensagens de comando e interface aceitam tags Adventure MiniMessage como
`<green>`, `<red>`, `<bold>` e `<#ff8800>`. Templates antigos com
`${green}` ou `$aqua` também continuam funcionando: os tokens de cores
legados são mapeados para tags MiniMessage. Variáveis como `$player`,
`$item` e `$amount` são escapadas para não injetar formatação.


## Nomes de itens sem downloads Mojang (etapa 4, #7)

As descrições de receitas passam o nome dos itens como componente Adventure, usando
placeholders na sintaxe MiniMessage. A tradução do item é resolvida pelo cliente
Minecraft. O FastCraft não busca mais índices/arquivos de idioma na Mojang ao iniciar
ou abrir a interface. Nomes e lores customizados dos itens são preservados como
componentes nativos (incluindo estilos e translatable). É recomendado validar as
receitas com NBT/components customizados após a atualização.

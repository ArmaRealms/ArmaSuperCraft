[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/BenWoodworth/FastCraft?include_prereleases)](https://github.com/BenWoodworth/FastCraft/releases)
[![Downloads](https://img.shields.io/github/downloads/BenWoodworth/FastCraft/total.svg)](https://github.com/BenWoodworth/FastCraft/releases)
[![Donate](https://img.shields.io/badge/stats-Bukkit-blue)](https://bstats.org/plugin/bukkit/FastCraft/1381)
[![Donate](https://img.shields.io/badge/donate-PayPal-yellow)](https://paypal.me/BenWoodworth)

# FastCraft

> **Platform requirement:** The new distribution targets **Paper 1.21.11** running on **Java 21**. Spigot, legacy Bukkit
> and older Minecraft versions are not supported.

The plugin entrypoint and its `paper-plugin.yml` are maintained in `fastcraft-paper/`. Use `./gradlew build` for the complete
build and `./gradlew runServer` for a local Paper 1.21.11 server.

**Migration status:** Only `fastcraft-api`, `fastcraft-core` and `fastcraft-paper` remain as Gradle modules; the former
Bukkit/Spigot modules and the bundled Spigot 1.7 API JAR are removed. Legacy version-suffixed implementation class names
have also been replaced with Paper-specific names. Build/test and Paper runtime validation of the consolidation are
still required; see [PAPER_ONLY_MIGRATION.md](PAPER_ONLY_MIGRATION.md).

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

Os comandos `/craft`, `/fastcraft` e `/fc` usam Brigadier, com registro via
`LifecycleEvents.COMMANDS` e permissões declaradas nos nós. Não existe mais
registro por `plugin.yml` ou pela API `PluginCommand`.

- `/craft`: abre a interface SuperCraft quando a preferência está ativa e o jogador tem `fastcraft.use`; caso contrário, abre a bancada vanilla, desde que possa usar o comando.
- `/craft alternar`: alterna a preferência pessoal.
- `/craft on`: ativa a preferência.
- `/craft off`: desativa a preferência (forma canônica; não existe `/craft offf`).

As permissões atuais `fastcraft.command.craft.fastcraft` e
`fastcraft.command.set.enabled` são reutilizadas. O uso da interface SuperCraft
também exige `fastcraft.use`, inclusive pelo comando. A interface vanilla é
permitida com `fastcraft.command.craft.grid` ou, ao usar `/craft` com a
preferência desativada, com `fastcraft.command.craft.fastcraft`.

Os comandos pessoais legados `/fastcraft craft [tipo]` e
`/fastcraft set enabled <opção>` foram removidos. O comando `/fastcraft`
(alias `/fc`) permanece **somente para administração**:

- `/fastcraft craft <fastcraft|grid|default> <jogador>`: abre uma interface
  para um jogador online.
- `/fastcraft set enabled <true|false|toggle> <jogador>`: altera a preferência
  de um jogador online.
- `/fastcraft reload`: recarrega a configuração.

Os dois primeiros comandos exigem um `<jogador>` explícito; sem ele, o
Brigadier considera o comando incompleto. O nome do jogador online é sugerido
pelo próprio comando. A permissão `fastcraft.use` continua necessária
para abrir o SuperCraft ao clicar na bancada física ou por comando.

Se outro plugin também registrar `/craft`, use o comando namespaced para
resolver a colisão e revise a prioridade dos plugins instalados.

## Mensagens MiniMessage e idiomas (etapa 3, #7)

O idioma global do plugin é configurado em `plugins/FastCraft/config.yml` com
`language: pt_BR` como padrão. Os arquivos `lang/*.json` do JAR são copiados
para `plugins/FastCraft/lang/` somente quando ausentes, preservando suas edições.
Chaves ausentes usam a versão do `en.json`. Modificações são reaplicadas com
`/fastcraft reload`.

Mensagens, títulos e descrições usam **exclusivamente** MiniMessage:
`<green>`, `<red>`, `<bold>`, `<#ff8800>` etc. As variáveis
usam tags MiniMessage como `<amount>`, `<item>`, `<page>`,
`<total>`, `<player>`, `<id>`, `<usage>` e `<permission>`.
Valores de usuários são inseridos com `Placeholder.unparsed`, e nomes de
itens como componentes com `Placeholder.component`.

Não há suporte a códigos de cores `§a` ou `&a`, nem tokens
`$item`, `${green}` ou `$player`. Arquivos de idioma com essas
sintaxes são considerados inválidos e recebem fallback em inglês.
Não existe migração automática dos arquivos antigos; atualize manualmente
os arquivos editáveis em `plugins/FastCraft/lang/` para MiniMessage.

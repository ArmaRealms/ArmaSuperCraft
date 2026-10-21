# SuperCraft

Plugin de criação rápida para servidores **Paper 1.21.11**, com **Java 21**. Ao interagir com uma bancada de trabalho, o jogador pode visualizar as receitas disponíveis com os itens do inventário e criar produtos diretamente pela interface do SuperCraft. O botão de bancada na interface permite acessar a grade vanilla 3×3.

A interface especial é aberta ao interagir com a bancada quando o jogador possui a permissão `armamc.supercraft.craft.use` e a preferência pessoal está ativa. Caso contrário, a bancada vanilla permanece disponível.

## Instalação

Coloque o JAR na pasta `plugins/` de um servidor Paper compatível e reinicie. Os dados do plugin ficam em `plugins/FastCraft/`:

- `config.yml`: preferências padrão, receitas excluídas e organização da interface.
- `lang/*.json`: textos editáveis da interface e mensagens, com formatação MiniMessage.
- `player-settings.dat`: preferências individuais persistidas dos jogadores (arquivo binário; não editar manualmente).

Use `/fastcraft reload` para reler as configurações e traduções. Para mudanças de versão do plugin, reinicie o servidor.

## Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/craft` | Abre o SuperCraft se a preferência estiver ativa; caso contrário, abre a bancada vanilla. | `armamc.supercraft.craft.use` |
| `/craft alternar` | Alterna a preferência pessoal. | `armamc.supercraft.craft.use` |
| `/craft on` | Ativa o SuperCraft para o jogador. | `armamc.supercraft.craft.use` |
| `/craft off` | Desativa o SuperCraft para o jogador. | `armamc.supercraft.craft.use` |
| `/fastcraft reload` | Recarrega configuração e traduções. | `armamc.supercraft.admin` |
| `/fastcraft craft <tipo> <jogador>` | Abre uma interface para um jogador online (`fastcraft`, `grid` ou `default`). | `armamc.supercraft.admin` |
| `/fastcraft set enabled <estado> <jogador>` | Define a preferência de outro jogador (`true`, `false` ou `toggle`). | `armamc.supercraft.admin` |

`/fc` é alias de `/fastcraft`. Os comandos administrativos com alvo exigem explicitamente `<jogador>`. Os comandos pessoais usam `/craft`.

### Conflitos de comandos e namespace

O Paper registra `/supercraft` como comando principal e `/craft` como alias no lifecycle Brigadier. Esse alias **não substitui** um `/craft` já registrado por outro plugin. Use `/supercraft` ou `/fastcraft:supercraft` como alternativa sem ambiguidade; ambos aceitam `alternar`, `on` e `off`. A alternativa administrativa é `/fastcraft:fastcraft`. Os registros não são duplicados no `paper-plugin.yml`.

### Permissões

O plugin registra **somente dois nós**:

- `armamc.supercraft.craft.use`: permite usar a interface especial ao clicar na bancada física, executar `/craft`, `/craft alternar`, `/craft on` e `/craft off` (e os aliases pessoais).
- `armamc.supercraft.admin`: permite executar os comandos administrativos `/fastcraft reload`, `/fastcraft craft ...` e `/fastcraft set enabled ...` (também por `/fc`).

Os dois nós são independentes: a permissão administrativa não concede automaticamente o uso pessoal. Mesmo com permissão administrativa, abrir explicitamente o SuperCraft para outro jogador exige que **o jogador-alvo** possua `armamc.supercraft.craft.use`. O modo `grid` continua disponível para o administrador sem essa permissão no alvo.

**Migração:** os nós antigos `fastcraft.*` e seus grupos foram removidos, sem aliases de compatibilidade. Atualize os grupos do gerenciador de permissões, por exemplo com LuckPerms:

```text
/lp group default permission set armamc.supercraft.craft.use true
/lp group admin permission set armamc.supercraft.admin true
```

Os comandos são registrados via Brigadier pela Command API do Paper.

## API de receitas delegadas (issue #21)

O SuperCraft suporta provedores opcionais de receitas com visibilidade **por jogador**,
quantidades exatas de ingredientes e transações executadas integralmente pelo plugin provedor.
A API Paper fica em `net.benwoodworth.fastcraft.bukkit.api.DelegatedRecipeProvider`
no artefato `fastcraft-paper`.

O consumidor registra seu provedor no `ServicesManager` do Bukkit/Paper. Exemplo Kotlin:

```kotlin
server.servicesManager.register(
    DelegatedRecipeProvider::class.java,
    myProvider,
    this, // instância do plugin provedor
    ServicePriority.Normal,
)
```

Remova o registro em `onDisable` com `server.servicesManager.unregister(myProvider)`.
Para um plugin Paper, declare dependência **server** de `FastCraft` com
`required: true`, `load: AFTER` e `join-classpath: true` no `paper-plugin.yml`,
garantindo acesso ao mesmo tipo público da API. O SuperCraft não requer o mcMMOAddon.

O contrato implementa:

- `claimedRecipeIds`: IDs de receitas vanilla que o provedor assume, **independentemente do jogador**. Todos esses IDs são excluídos do processamento vanilla e dos eventos sintéticos. IDs reivindicados por mais de um provedor não são exibidos. Um ID já reivindicado permanece reservado até reiniciar o servidor, mesmo depois da remoção/desativação do serviço.
- `getOffers(player)`: ofertas disponíveis ao jogador, com `id` (`NamespacedKey`), `result` (`ItemStack`) e `ingredients` (`List<ItemStack>`). Os `amount` individuais são quantidades por craft, não slots vanilla com quantidade 1; os metadados completos são preservados. O SuperCraft cria cópias dos itens.
- `craft(player, id, repetitions, dropResults)`: execução **síncrona na thread principal**. `repetitions` é entre 1 e 64 e representa uma transação indivisível, jamais uma série de crafts parcialmente confirmados. O provedor revalida jogador, nível, permissão, receitas, conteúdo exato do inventário (ItemMeta/PDC), quantidades e destino antes de qualquer alteração.
- `DelegatedCraftResult.SUCCESS`, `REJECTED` ou `UNAVAILABLE`: a operação só pode retornar `SUCCESS` depois de consumir todos os ingredientes e entregar todos os resultados com sucesso. Falhas não podem modificar o inventário, conceder resultados nem largar itens. O SuperCraft **não consome, entrega, refaz ou compensa** itens para receitas delegadas.

A oferta é reconsultada na execução e é atualizada na abertura da GUI, no botão de refresh
e nas alterações de inventário detectadas. Mudanças de nível sem alteração de inventário
podem ser atualizadas pelo botão de refresh; **a autorização é sempre revalidada em `craft`**.
O SuperCraft impede reentrância e atualiza a GUI após toda tentativa de craft.
O contrato transacional exige implementação correta pelo provedor; nenhum sistema externo
consegue desfazer com segurança mutações arbitrárias feitas por um provedor que violar esse contrato.

**Compatibilidade e mitigação:** enquanto o mcMMOAddon não implementar esse provedor
(ArmaPlugins#1021), mantenha em `plugins/FastCraft/config.yml`:

```yaml
disable-recipes:
  recipe-ids:
    - "mcmmoaddon:*"
```

Essa exclusão permanece necessária até a implantação e validação conjunta no Paper 1.21.11;
retire-a apenas para liberar as ofertas delegadas após confirmar registro, consumos,
limites de pilhas, falhas e testes em servidor. `Player#discoverRecipe` não substitui
autorização nem commit transacional.

## Configuração (`config.yml`)

O plugin cria e completa os campos ausentes no arquivo `plugins/FastCraft/config.yml`. Exemplo com os valores padrão:

```yaml
language: pt_BR

disable-recipes:
  recipe-ids: [ ]

recipe-calculations:
  max-tick-usage: 0.1

player-defaults:
  enabled: true

layout:
  height: 6

  recipes:
    row: 0
    column: 0
    width: 7
    height: 6

  buttons:
    crafting-grid:
      enable: true
      item: minecraft:crafting_table
      row: 0
      column: 8
    craft-amount:
      enable: true
      item: minecraft:anvil
      row: 1
      column: 8
    refresh:
      enable: true
      item: minecraft:nether_star
      row: 2
      column: 8
    page:
      enable: true
      item: minecraft:iron_sword
      row: 5
      column: 8

  items:
    custom-button-example:
      enable: false
      item: minecraft:stone_button
      row: 3
      column: 8
      width: 1
      height: 1
      command: "say <player> pressed a custom button"

  background:
    enable: false
    item: minecraft:light_gray_stained_glass_pane
```

### Campos principais

| Campo | Padrão | O que controla |
| --- | --- | --- |
| `language` | `pt_BR` | Idioma global das mensagens e da interface. Aceita códigos de idioma correspondentes aos arquivos JSON em `lang/`. |
| `disable-recipes.recipe-ids` | `[]` | Lista de IDs/padrões de receitas que não devem aparecer para criação rápida. |
| `recipe-calculations.max-tick-usage` | `0.1` | Limite de tempo relativo de processamento de receitas por tick; deve ser um número igual ou maior que zero. |
| `player-defaults.enabled` | `true` | Preferência inicial quando o jogador ainda não definiu `/craft on` ou `/craft off`. Não sobrescreve preferências individuais salvas. |
| `layout.height` | `6` | Número de linhas da interface, entre 1 e 6. |
| `layout.recipes.row` | `0` | Linha inicial da área de receitas; contagem a partir de zero. |
| `layout.recipes.column` | `0` | Coluna inicial da área de receitas; contagem a partir de zero. |
| `layout.recipes.width` | `7` | Largura da área de receitas, limitada pelas colunas disponíveis (máximo 9). |
| `layout.recipes.height` | `6` | Altura da área de receitas, em linhas. |

Em `disable-recipes.recipe-ids`, uma entrada literal corresponde a um ID exato; `*` funciona como curinga. Uma expressão entre barras (`/expressão/`) é interpretada como regex. Exemplos:

```yaml
disable-recipes:
  recipe-ids:
    - minecraft:diamond_sword
    - "minecraft:*_pickaxe"
    - "/^minecraft:.*_helmet$/"
```

### Botões e layout

Os botões existentes ficam em `layout.buttons`:

| Campo | O que controla |
| --- | --- |
| `crafting-grid` | Botão para abrir a bancada vanilla 3×3. |
| `craft-amount` | Botão para modificar a quantidade de itens criados. |
| `refresh` | Atualiza a lista de receitas da interface. |
| `page` | Navega entre páginas de receitas. |

Cada botão oferece os campos `enable` (mostrar ou ocultar), `item` (material Minecraft), `row` (linha, a partir de 0) e `column` (coluna, de 0 a 8). As linhas válidas dependem de `layout.height`. **Todos os botões habilitados precisam ocupar slots distintos**, incluindo botões personalizados. Um layout com colisões, ou sem ao menos um slot livre na área de receitas, é rejeitado no startup/reload; o reload preserva a configuração anterior.

Por exemplo, ao reduzir a interface para duas linhas, os valores padrão de `craft-amount`, `refresh` e `page` passam a coincidir após o ajuste das linhas. É necessário posicioná-los explicitamente, como neste trecho:

```yaml
layout:
  height: 2
  buttons:
    crafting-grid:
      row: 0
      column: 8
    craft-amount:
      row: 1
      column: 8
    refresh:
      row: 0
      column: 7
    page:
      row: 1
      column: 7
```

Em `layout.background`, `enable` ativa ou desativa o preenchimento visual e `item` define o material usado como fundo.

### Botões personalizados (`layout.items`)

Cada chave em `layout.items` representa um botão adicional. Os campos disponíveis são:

| Campo | O que controla |
| --- | --- |
| `enable` | Exibe ou oculta o botão (padrão: `false`). |
| `item` | Material usado como ícone (padrão: `minecraft:stone_button` no botão de exemplo). |
| `row` e `column` | Posição inicial na interface (contagem a partir de 0). |
| `width` e `height` | Dimensões ocupadas pelo botão; padrão: 1×1. |
| `command` | Comando opcional executado quando o jogador clica no botão. |

As dimensões `width` e `height` devem ser maiores que zero e respeitar os limites da grade: `column + width <= 9` e `row + height <= layout.height`. Dimensões inválidas impedem a inicialização ou fazem o `/fastcraft reload` falhar, mantendo a configuração anterior em memória. Evite sobrepor botões. O item de exemplo vem desativado e pode ser editado ou substituído por novas chaves.

## Idiomas e mensagens (`lang/*.json`)

No primeiro carregamento, os catálogos distribuídos com o plugin são copiados para `plugins/FastCraft/lang/`, sem sobrescrever arquivos existentes. O idioma selecionado em `language` é **global**: a configuração do servidor determina as mensagens do plugin, independentemente do idioma individual de cada jogador. Nomes vanilla de itens são traduzidos pelo cliente Minecraft. O plugin **não baixa** manifestos, índices ou arquivos de tradução da Mojang durante a execução; o antigo cache `plugins/FastCraft/cache/assets/` não é mais utilizado e pode ser removido manualmente se existir.

Para alterar textos, edite `plugins/FastCraft/lang/pt_BR.json` (ou o JSON correspondente a `language`) e execute `/fastcraft reload`. Chaves ausentes utilizam o valor em `en.json`. Um arquivo inválido também aciona o fallback; erros são registrados no console.

### Chaves disponíveis

| Prefixo / chave | Finalidade |
| --- | --- |
| `gui.title` | Título da interface principal. |
| `gui.recipe.ingredients` e `gui.recipe.ingredients.item` | Cabeçalho e linhas dos ingredientes. |
| `gui.recipe.results` e `gui.recipe.results.item` | Cabeçalho e linhas dos resultados. |
| `gui.recipe.id` | Identificador técnico da receita. |
| `gui.button.page.title` | Título do controle de páginas. |
| `gui.button.page.description.0`, `.1`, `.2` | Instruções de clique do controle de páginas. |
| `gui.button.refresh.title` e `gui.button.refresh.description.0` | Título e descrição do botão de atualização. |
| `gui.button.craft-amount.title` e `gui.button.craft-amount.description.0`, `.1`, `.2` | Título e instruções do controle de quantidade. |
| `gui.button.crafting-grid.title` e `gui.button.crafting-grid.description.0`, `.1` | Título e descrição da bancada vanilla. |
| `command.error.usage` | Mensagem de uso incorreto do comando. |
| `command.error.permission` | Falta de permissão. |
| `command.error.console.only` | Comando reservado ao console. |
| `command.error.player.only` | Comando que exige jogador. |
| `command.error.player.unknown` | Jogador-alvo desconhecido. |
| `command.set.enabled.true` e `command.set.enabled.false` | Confirmações de ativação/desativação pessoal. |
| `command.set.enabled.true.player` e `command.set.enabled.false.player` | Confirmações de alteração da preferência de outro jogador. |
| `command.reload.reloaded` | Confirmação de recarregamento. |
| `localization.translator` e `localization.translator.from` | Créditos de tradução distribuídos no catálogo. |

### Formatação MiniMessage

Todas as mensagens configuráveis usam **MiniMessage** (Adventure). Exemplo:

```json
{
  "gui.title": "<gold><bold>SuperCraft</bold></gold>",
  "gui.recipe.ingredients.item": "<green><amount>x <item>",
  "command.error.permission": "<red>Sem permissão: <permission>"
}
```

Placeholders disponíveis conforme a mensagem: `<amount>` (quantidade), `<item>` (nome do item), `<page>` (página atual), `<total>` (total de páginas), `<id>` (identificador de receita), `<player>` (jogador), `<usage>` (sintaxe de comando) e `<permission>` (permissão exigida).

Preserve os placeholders previstos em cada mensagem. Valores dinâmicos são inseridos com placeholders seguros, sem interpretar o conteúdo fornecido pelo jogador como tags.

Não use códigos de cor legados `&a` ou `§a`, nem placeholders antigos de cifrão: esses formatos não são aceitos nos arquivos de idioma. Use tags como `<green>`, `<red>`, `<bold>` e `<#ff8800>` em vez disso.

Em botões personalizados, o campo `command` é um **comando literal de console**, não uma mensagem MiniMessage. Apenas o marcador `<player>` é substituído pelo nome do jogador. Outros caracteres e argumentos, inclusive textos entre `<` e `>`, são preservados sem interpretar tags de formatação. A opção `language` não modifica o idioma com que o Minecraft traduz nomes vanilla no inventário.


## Validação em servidor

Antes de integrar uma alteração importante, execute `./gradlew :fastcraft-paper:test --stacktrace`, `./gradlew clean build --stacktrace` e `./gradlew verifyShadedInjectionApi`, depois faça um teste manual no Paper 1.21.11 / Java 21 e, se disponível, num fork compatível (por exemplo, Leaf). Com `./gradlew runServer`, confira:

- `/craft`, `/craft alternar`, `/craft on`, `/craft off`, `/fastcraft` e `/fc` com jogador autorizado, jogador sem permissão e console; verifique as sugestões e o namespace `/fastcraft:supercraft`.
- Reinício e reload preservam preferências individuais, alterações em `config.yml`, mensagens MiniMessage personalizadas e fallback de chaves para inglês.
- Cliente em português e outro idioma exibem os nomes vanilla traduzidos por seus próprios recursos; nomes e lore personalizados, cores, hover e placeholders continuam intactos, sem downloads de arquivos da Mojang.
- Receitas shaped, shapeless e especiais, de plugins e datapacks, funcionam sem dupe; teste shift-click, inventário cheio e crafting cancelado.
- Clique na bancada física, chamada virtual por outro plugin, clique duplo e botão de retorno para a bancada vanilla não causam aberturas duplicadas nem ignoram `armamc.supercraft.craft.use` ou `/craft off`.

Este roteiro não substitui a execução real, especialmente para eventos de inventário que não sejam reproduzíveis fielmente no MockBukkit.

### Validação específica da bancada física (issue #16)

1. Com um jogador não-OP e `armamc.supercraft.craft.use`, acione uma bancada física com `/craft on`. Deve aparecer **uma** GUI SuperCraft. Repita sem a permissão, após `/craft off` e após `/craft alternar` desativado: nesses casos deve aparecer a bancada vanilla.
2. Teste mão principal e secundária, clique esquerdo, clique direito e clique repetido no mesmo tick. Eventos cancelados por plugins de proteção não devem disparar o SuperCraft.
3. Abra uma bancada virtual pelo comando `/craft` ou por outro plugin, com e sem interação física recente, incluindo um teste em que outro plugin chama a abertura de `WORKBENCH` durante `PlayerInteractEvent`. Ela não deve ser substituída acidentalmente.
4. Use o botão da bancada dentro da GUI, faça dois cliques rápidos, feche a GUI antes do próximo tick e teste desconexão. A abertura deve ocorrer no tick seguinte somente quando a GUI original ainda estiver ativa, sem loop ou múltiplos menus.
5. Repita em Paper 1.21.11/Java 21 e em fork compatível. Registre os logs e o resultado da interação física versus a abertura virtual; o MockBukkit não reproduz a ordem exata de todos os eventos do servidor.



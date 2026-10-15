# SuperCraft

Plugin de criação rápida para servidores **Paper 1.21.11**, com **Java 21**. Ao interagir com uma bancada de trabalho, o jogador pode visualizar as receitas disponíveis com os itens do inventário e criar produtos diretamente pela interface do SuperCraft. O botão de bancada na interface permite acessar a grade vanilla 3×3.

A interface especial é aberta ao interagir com a bancada quando o jogador possui a permissão `fastcraft.use` e a preferência pessoal está ativa. Caso contrário, a bancada vanilla permanece disponível.

## Instalação

Coloque o JAR na pasta `plugins/` de um servidor Paper compatível e reinicie. Os dados do plugin ficam em `plugins/FastCraft/`:

- `config.yml`: preferências padrão, receitas excluídas e organização da interface.
- `lang/*.json`: textos editáveis da interface e mensagens, com formatação MiniMessage.
- `player-settings.dat`: preferências individuais persistidas dos jogadores (arquivo binário; não editar manualmente).

Use `/fastcraft reload` para reler as configurações e traduções. Para mudanças de versão do plugin, reinicie o servidor.

## Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/craft` | Abre o SuperCraft quando autorizado e ativado; caso contrário, abre a grade vanilla para jogadores autorizados. | `fastcraft.command.craft.fastcraft` ou `fastcraft.command.craft.grid` |
| `/craft alternar` | Alterna a preferência pessoal entre ativada e desativada. | `fastcraft.command.set.enabled` |
| `/craft on` | Ativa a preferência pessoal. | `fastcraft.command.set.enabled` |
| `/craft off` | Desativa a preferência pessoal. | `fastcraft.command.set.enabled` |
| `/fastcraft reload` | Recarrega `config.yml` e os arquivos de idioma. | `fastcraft.admin.command.reload` |
| `/fastcraft craft <tipo> <jogador>` | Abre a interface escolhida para um jogador online. Tipos: `fastcraft`, `grid` ou `default` (respeita a preferência). | `fastcraft.admin.command.craft` |
| `/fastcraft set enabled <estado> <jogador>` | Define a preferência de um jogador online: `true`, `false` ou `toggle` (alterna). | `fastcraft.admin.command.set.enabled` |

`/fc` é um alias de `/fastcraft` para administração. Os comandos administrativos com alvo exigem explicitamente `<jogador>`. Não há comandos pessoais em `/fastcraft`; utilize `/craft` e seus subcomandos.

### Permissões

- `fastcraft.use`: permite substituir a bancada física pelo SuperCraft e abrir a interface especial por comando; não concede, por si só, acesso a `/craft`.
- `fastcraft.command.craft.fastcraft`: habilita a abertura do SuperCraft via `/craft`, desde que o jogador também possua `fastcraft.use`. Ao desativar a preferência, `/craft` pode abrir a grade vanilla com essa permissão.
- `fastcraft.command.craft.grid`: permite abrir a grade vanilla via `/craft`.
- `fastcraft.command.set.enabled`: permite modificar a própria preferência.
- `fastcraft.admin.command.craft`: permite abrir a bancada para outro jogador online.
- `fastcraft.admin.command.set.enabled`: permite alterar a preferência de outro jogador online.
- `fastcraft.admin.command.reload`: permite recarregar as configurações e traduções.

Há também os grupos de permissões `fastcraft.command.craft.*`, `fastcraft.command.set.*`, `fastcraft.command.*`, `fastcraft.admin.command.set.*`, `fastcraft.admin.command.*`, `fastcraft.admin.*` e `fastcraft.*`, que agrupam as permissões individuais correspondentes.

Mesmo com acesso administrativo, abrir explicitamente a interface especial de um jogador exige que **o jogador-alvo** possua `fastcraft.use`. Os comandos são registrados com Brigadier pela Command API do Paper.

## Configuração (`config.yml`)

O plugin cria e completa os campos ausentes no arquivo `plugins/FastCraft/config.yml`. Exemplo com os valores padrão:

```yaml
language: pt_BR

disable-recipes:
  recipe-ids: []

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

Cada botão oferece os campos `enable` (mostrar ou ocultar), `item` (material Minecraft), `row` (linha, a partir de 0) e `column` (coluna, de 0 a 8). As linhas válidas dependem de `layout.height`.

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

No primeiro carregamento, os catálogos distribuídos com o plugin são copiados para `plugins/FastCraft/lang/`, sem sobrescrever arquivos existentes. O idioma selecionado em `language` é **global**: a configuração do servidor determina as mensagens do plugin, independentemente do idioma individual de cada jogador. Nomes vanilla de itens são traduzidos pelo cliente Minecraft.

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

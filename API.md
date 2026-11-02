# AnarchyTags — API para desenvolvedores

Integração com outros plugins Bukkit/Paper/Folia via **ServicesManager** e classes em `anarchy.minesky.tags.api`.

## Dependência

No `plugin.yml` do seu plugin:

```yaml
softdepend: [AnarchyTags]
```

No código, use sempre `AnarchyTagsProvider` — nunca assuma que o AnarchyTags está online.

---

## Obter a API

```java
import anarchy.minesky.tags.api.AnarchyTagsAPI;
import anarchy.minesky.tags.api.AnarchyTagsProvider;

AnarchyTagsAPI api = AnarchyTagsProvider.get();
if (api == null) {
    // AnarchyTags desligado ou ainda carregando
    return;
}

// Ou falha explícita (IllegalStateException se indisponível):
AnarchyTagsAPI api = AnarchyTagsProvider.require();
```

A implementação registrada é a mesma exposta em `AnarchyTags.getAPI()`.

---

## `AnarchyTagsAPI` — métodos

### `refreshTag(Player target)`

Força recálculo e envio do texto da tag 3D de `target` para **todos os viewers** que já estão vendo essa tag.

Use quando placeholders externos mudarem (ex.: saldo, clã) e você não quiser esperar o `nametags.update-interval` da config.

```java
api.refreshTag(jogador);
```

---

### `setCustomFormat(Player player, String format)`

Define **uma linha** de formato exclusiva para aquele jogador (substitui temporariamente as linhas globais do `config.yml`). Passe `null` ou string vazia para voltar ao padrão do servidor.

Aceita placeholders relacional (viewer → target) como na config normal, incluindo `{player}` e `{voiceicon}`.

```java
api.setCustomFormat(jogador, "<gold>VIP %luckperms_prefix%{player}");
api.setCustomFormat(jogador, null); // remove override
```

---

### `reload()`

Recarrega `config.yml`, recria tags dos jogadores online e reinicia schedulers internos. Equivalente a `/anarchytags reload`.

```java
api.reload();
```

---

### `registerVisibilityFilter(NametagVisibilityFilter filter)`

### `unregisterVisibilityFilter(NametagVisibilityFilter filter)`

Registra um filtro que decide se a tag 3D aparece para um par **viewer → target**. Thread-safe; pode registrar/desregistrar no `onEnable`/`onDisable`.

Regras:

- Vários filtros podem coexistir.
- Ordem: maior `priority()` primeiro.
- Se **qualquer** filtro retornar `false`, a tag fica oculta para aquele par.
- Filtros **não** substituem distância, vanish, modo invisível do `/escondernick`, etc. — eles são aplicados **junto** com a lógica interna.

Interface:

```java
public interface NametagVisibilityFilter {
    boolean shouldShow(Player viewer, Player target);
    default int priority() { return 0; }
}
```

---

### `isNametagVisibleFor(Player viewer, Player target)`

Retorna `true` se a tag de `target` **deveria** ser visível para `viewer` agora, considerando:

- distância (`visibility-distance`),
- `show-self`, spectator, vanish,
- modo `/escondernick invisivel`,
- **todos** os `NametagVisibilityFilter` registrados.

Útil para alinhar UI (mapa, scoreboard) com o que o jogador vê no mundo.

```java
if (api.isNametagVisibleFor(viewer, alvo)) {
    // ...
}
```

---

### `refreshVisibilityFor(Player target)`

Reavalia **todos** os viewers online em relação à tag de `target`: mostra quem passou a ver, esconde quem falhou no filtro/distância, atualiza texto se necessário.

Chame sempre que o estado do seu fog of war (ou zona) mudar para aquele jogador ou para o mapa inteiro.

```java
// Jogador saiu da névoa / entrou em visão de outro
api.refreshVisibilityFor(jogador);
```

Para mudança global, itere online players ou só os afetados:

```java
for (Player p : Bukkit.getOnlinePlayers()) {
    api.refreshVisibilityFor(p);
}
```

---

### `getNickHideMode(Player player)`

Retorna o modo persistido de esconder nick:

| Valor | Significado |
|--------|-------------|
| `OFF` | Normal |
| `OBFUSCATED` | Ofuscado (`/escondernick`) |
| `FULL_HIDDEN` | Tag oculta (`/escondernick invisivel`) |

```java
import anarchy.minesky.tags.nick.NickHideMode;

NickHideMode mode = api.getNickHideMode(player);
if (mode == NickHideMode.FULL_HIDDEN) { ... }
```

---

### `isNickLineEnabled(Player player)`

Se a linha `{player}` na tag está ativa. Quando `false` (via `/escondernick habilitar`), prefixo LuckPerms, economia e clãs **permanecem**; só o nome some.

```java
if (!api.isNickLineEnabled(player)) { ... }
```

---

## Exemplo: Fog of War

```java
public final class MeuFogPlugin extends JavaPlugin {

    private NametagVisibilityFilter fogFilter;

    @Override
    public void onEnable() {
        AnarchyTagsAPI tags = AnarchyTagsProvider.get();
        if (tags == null) {
            getLogger().warning("AnarchyTags ausente — tags sem fog of war.");
            return;
        }

        fogFilter = new NametagVisibilityFilter() {
            @Override
            public boolean shouldShow(Player viewer, Player target) {
                return meuFogService.canSee(viewer, target);
            }

            @Override
            public int priority() {
                return 100;
            }
        };
        tags.registerVisibilityFilter(fogFilter);
    }

    @Override
    public void onDisable() {
        AnarchyTagsAPI tags = AnarchyTagsProvider.get();
        if (tags != null && fogFilter != null) {
            tags.unregisterVisibilityFilter(fogFilter);
        }
    }

    public void onFogUpdated(Player target) {
        AnarchyTagsAPI tags = AnarchyTagsProvider.get();
        if (tags != null) {
            tags.refreshVisibilityFor(target);
        }
    }
}
```

---

## Placeholders e voz (referência)

Não fazem parte da interface Java, mas costumam aparecer junto da API:

| Token | Uso |
|--------|-----|
| `{player}` | Nome do dono da tag (respeita ofuscado / linha desabilitada) |
| `{voiceicon}` | Ícone de voz na linha da tag |
| `%atvc_icon%` | PlaceholderAPI (expansion interna) |
| `%atvc_state%` | Estado: `talking`, `whispering`, `quiet`, `group`, `disabled`, `not_installed` |
| `%atvc_group%` | Nome do grupo do Simple Voice Chat, ou vazio |

**Ícone de voz:** mudanças (falar, mutar, conectar) atualizam a tag **na hora**, independente de `nametags.update-interval`. Esse intervalo só limita o refresh **periódico** de placeholders pesados (economia, clãs, etc.).

---

## Comandos e permissões (jogadores)

| Comando | Permissão |
|---------|-----------|
| `/escondernick` | `anarchytags.nick.ofuscado` |
| Ver nick `&8` em ofuscado | `anarchytags.nick.ofuscado.view` |
| `/escondernick habilitar` | (todos) |
| `/escondernick invisivel` | `anarchytags.nick.invisible` |
| `/escondernick invisivel <jogador>` | `anarchytags.nick.invisible.others` |
| `/anarchytags reload` | `anarchytags.admin` |

---

## Boas práticas

1. **Null-check** em `AnarchyTagsProvider.get()` no `onEnable`.
2. **Unregister** filtros no `onDisable`.
3. Após mudar visibilidade no seu plugin, chame `refreshVisibilityFor` (não basta alterar só o mapa interno).
4. Use `refreshTag` para texto; use `refreshVisibilityFor` para mostrar/esconder entidades.
5. Em Folia, a API já agenda trabalho nas regiões corretas — não chame pacotes de display manualmente.

---

## Pacotes

| Classe | Pacote |
|--------|--------|
| `AnarchyTagsAPI` | `anarchy.minesky.tags.api` |
| `AnarchyTagsProvider` | `anarchy.minesky.tags.api` |
| `NametagVisibilityFilter` | `anarchy.minesky.tags.api` |
| `NickHideMode` | `anarchy.minesky.tags.nick` |

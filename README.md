# Skin Camo — Mod para Minecraft Forge 1.20.1

Mod que faz todo jogador nascer com uma skin **totalmente branca** e oferece uma
interface in-game (tecla **P**) para pintar essa skin em tempo real, parte por
parte, inclusive copiando cores do ambiente com um conta-gotas — tudo
sincronizado entre jogadores em multiplayer e salvo entre sessões.

> **Importante sobre este pacote**: todo o código abaixo foi escrito por
> completo, mas **não foi compilado nem testado** neste ambiente, porque o
> sandbox onde ele foi gerado não tem acesso à internet (não consegue baixar
> Forge/Gradle/MDK) nem um cliente de Minecraft para rodar. O código segue
> fielmente as APIs do Forge/Mojang mappings para 1.20.1 e foi revisado
> manualmente várias vezes, mas **você precisa compilar localmente** (passo a
> passo abaixo) e pode ser necessário corrigir pequenos detalhes, em especial
> no ponto marcado como "maior risco técnico" mais abaixo.
>
> **Único arquivo faltando**: `gradle/wrapper/gradle-wrapper.jar` (binário,
> não dá pra gerar sem internet). Veja
> `gradle/wrapper/LEIA-ME_gradle-wrapper-jar.txt` — é 1 comando só
> (`gradle wrapper --gradle-version 8.1.1`) e o projeto fica 100% completo.
> O repositório já vem com `git init` feito, `.gitignore`, `.gitattributes` e
> `LICENSE` (MIT) prontos para ir direto ao GitHub — veja a última seção
> deste README.

---

## 1. Como compilar e rodar

Pré-requisitos: **JDK 17** instalado e internet disponível (para o Gradle
baixar o Forge MDK e as dependências na primeira execução).

```bash
# dentro da pasta do projeto (onde está o build.gradle)

# Linux/macOS
./gradlew genIntellijRuns      # opcional, se for abrir no IntelliJ
./gradlew runClient            # baixa o Forge, compila e abre o jogo com o mod carregado

# Windows
gradlew.bat runClient
```

Se preferir gerar apenas o `.jar` para colocar na pasta `mods` de uma instância
normal do Forge 1.20.1:

```bash
./gradlew build
# o jar fica em build/libs/skincamo-1.0.0.jar
```

Para testar o multiplayer, rode `./gradlew runServer` em paralelo (ou suba o
jar num servidor Forge 1.20.1-47.2.0 separado) e conecte com `runClient` em
duas instâncias.

O wrapper do Gradle (`gradlew`/`gradlew.bat`) normalmente já vem junto do
projeto quando gerado pelo MDK oficial; se este zip não tiver os arquivos
binários do wrapper (`gradle/wrapper/gradle-wrapper.jar`), baixe o **Forge
MDK 1.20.1-47.2.0** oficial, copie a pasta `gradle/` dele para dentro deste
projeto, e rode `gradle wrapper` uma vez.

---

## 2. Visão geral da arquitetura

| Pacote | Responsabilidade |
|---|---|
| `capability/` | Guarda, **no servidor**, a cor pintada de cada uma das 6 partes do corpo de cada jogador. Persistida automaticamente via NBT da capability (sobrevive a relog/restart). |
| `network/` | 3 pacotes customizados (`PaintPartPacket`, `FillAllPacket` cliente→servidor; `SyncSkinDataPacket` servidor→clientes). Servidor é a fonte da verdade. |
| `server/` | Sincroniza o estado de pintura quando um jogador entra (ele recebe a aparência de todo mundo; todo mundo recebe a aparência dele). |
| `client/` | Renderização (textura dinâmica), GUI de pintura, conta-gotas, teclas, animação de "tinta se espalhando". |
| `mixin/` | Único ponto de engenharia mais delicada: troca a textura que o jogo usa para renderizar cada jogador. |

### Fluxo de pintura
1. Jogador escolhe uma cor na GUI (ou usa o conta-gotas/Auto-Camuflagem).
2. Cliente aplica a cor **imediatamente** na sua própria tela (resposta
   instantânea, sem esperar rede) e manda um pacote pro servidor.
3. Servidor valida, atualiza a capability daquele jogador e **retransmite**
   a confirmação para todos os clientes conectados (inclusive quem pintou),
   garantindo que todo mundo veja a mesma coisa mesmo se o pacote otimista
   local e a confirmação do servidor divergirem por algum motivo.

### Como a textura é trocada sem precisar relogar
Cada jogador tem uma `DynamicTexture`/`NativeImage` 64×64 criada uma única vez
(`SkinCamoTextureManager`), que começa branca com a camada de overlay
totalmente transparente. Pintar uma parte só reescreve os pixels daquele
retângulo específico (cabeça, tronco, braço, perna...) e faz upload
incremental pra GPU — não recria a textura do zero. O mixin
`PlayerRendererMixin` intercepta a escolha de qual textura desenhar para cada
jogador e devolve essa textura dinâmica em vez da skin baixada da
Mojang/servidor de skins.

### Conta-gotas, Auto-Camuflagem e Mimetismo
Em vez de ler pixel a pixel do atlas de blocos (caro e frágil entre versões),
o conta-gotas usa `BlockState#getMapColor()` — a mesma cor que o jogo usa para
desenhar mapas. Isso já leva bioma em conta (grama, folhas e água mudam de cor
junto com o bioma) e funciona automaticamente com blocos de qualquer outro
mod, já que todo `Block` tem uma `MapColor` padrão.
- **Conta-gotas (tecla G ou botão na GUI)**: captura a cor do bloco no centro
  da tela e abre a paleta já com essa cor selecionada.
- **Auto-Camuflagem**: faz a média de cor de um cubo de blocos ao redor do
  jogador (raio 3, ±1 de altura) e aplica na skin inteira de uma vez.
- **Mimetismo**: liga um modo contínuo que reamostra essa mesma média a cada
  ~0,5s **somente se o jogador se moveu o suficiente** e só reaplica se a cor
  mudou de forma perceptível — para não gerar tráfego de rede ou uploads de
  textura desnecessários.

### Persistência
- **Cor "oficial" da skin** (a que todo mundo vê): capability do servidor,
  salva automaticamente no NBT do jogador.
- **Preferências de interface** (histórico de cores, favoritos, opacidade):
  arquivo JSON simples em `config/skincamo/<uuid-do-jogador>.json`,
  independente da capability — é só conveniência de UI, então fica 100%
  client-side.

---

## 3. ⚠️ Ponto de maior risco técnico

Arquivo: `src/main/java/com/skincamo/mixin/PlayerRendererMixin.java`

Esse mixin intercepta `PlayerRenderer#getTextureLocation(AbstractClientPlayer)`
— o método que decide, a cada frame, qual textura usar para desenhar um
jogador — e substitui pela nossa textura dinâmica. Esse nome de método é
estável nos mappings oficiais da Mojang para 1.20.1, mas é o tipo de coisa que
só um build real confirma 100%.

**Se o build falhar especificamente nesse arquivo** (erro do tipo "method not
found" ou erro de remap do Mixin):
1. Abra a classe `net.minecraft.client.renderer.entity.player.PlayerRenderer`
   decompilada na sua IDE (com o MDK importado) e confirme o nome/assinatura
   real do método nessa build exata do Forge.
2. Ajuste a string `method = "..."` no `@Inject` para bater exatamente.
3. Todo o resto do mod (rede, capability, GUI, conta-gotas) **não depende**
   desse mixin para compilar — só a aplicação visual da textura nos outros
   jogadores depende dele.

---

## 4. O que está totalmente implementado vs. simplificado

| Recurso pedido | Status |
|---|---|
| Skin branca ao nascer | ✅ Completo |
| GUI de pintura (tecla P) com seleção de parte do corpo | ✅ Completo |
| Picker HSV (quadrado de saturação/valor + barra de matiz) | ✅ Completo |
| Campo hex + leitura RGB | ✅ Completo |
| Histórico de cores e favoritos | ✅ Completo (persistido em JSON) |
| Opacidade | ⚠️ Implementado como controle de UI e persistido, mas **não afeta
  a renderização ainda** — o formato de skin do Minecraft não suporta
  transparência na camada base; serve hoje como ponto de expansão (ex.: para
  uma futura camada de overlay translúcida). |
| Preenchimento total / por parte | ✅ Completo |
| Aplicação instantânea, sem relog | ✅ Completo (textura dinâmica + mixin) |
| Sincronização multiplayer em tempo real | ✅ Completo |
| Novos jogadores veem o estado atual de todos | ✅ Completo |
| Conta-gotas (clicar em bloco/bioma/folhagem/água) | ✅ Completo, via `MapColor` (ver seção de simplificações abaixo) |
| Conta-gotas funciona com blocos de outros mods | ✅ Completo (todo bloco tem `MapColor` padrão) |
| Tecla rápida G (conta-gotas instantâneo + aplica tudo) | ✅ Completo |
| Auto-Camuflagem (uma cor, baseada nos blocos próximos) | ✅ Completo |
| Modo Mimetismo (contínuo, ao se mover) | ✅ Completo, com throttling de rede/CPU |
| Animação de "tinta se espalhando" | ✅ Completo (revelação progressiva client-side, ~0,3s) |
| Persistência entre sessões | ✅ Completo (capability NBT + JSON de preferências) |
| Compatível com OptiFine/shaders | ❓ **Não testável aqui.** A textura é registrada
  normalmente no `TextureManager` do Minecraft, então deve se comportar como
  qualquer skin para a maioria dos shaders, mas alguns packs de shader fazem
  suposições especiais sobre a textura de jogador — só um teste real confirma. |

### Simplificação deliberada: conta-gotas por `MapColor`, não pixel-perfeito
Usar a cor de mapa de cada bloco é muito mais simples e robusto entre versões
e mods do que ler o atlas de texturas pixel a pixel via UV do
`BlockHitResult`. A troca é: você pega "a cor representativa do bloco" em vez
de "o pixel exato onde o crosshair tocou dentro da textura daquele bloco".
Para a maioria dos casos de camuflagem isso é visualmente equivalente. Quem
quiser precisão pixel-perfeita pode trocar `Eyedropper.sampleColorAt()` por
uma leitura do `TextureAtlasSprite` correspondente — fica documentado como
ponto de expansão futura no próprio código.

---

## 5. Estrutura de arquivos

```
src/main/java/com/skincamo/
├── SkinCamoMod.java                  (ponto de entrada)
├── capability/                       (estado persistido da skin pintada)
├── network/                          (pacotes cliente<->servidor)
│   └── packet/
├── server/                           (sincronização ao logar)
├── client/                           (renderização, teclas, GUI)
│   ├── gui/                          (tela de pintura + persistência de UI)
│   └── eyedropper/                   (amostragem de cor + modo mimetismo)
└── mixin/                            (troca da textura renderizada)

src/main/resources/
├── META-INF/mods.toml
├── skincamo.mixins.json
├── pack.mcmeta
└── assets/skincamo/lang/             (en_us.json, pt_br.json)
```

---

## 6. Testando rapidamente

1. `./gradlew runClient` duas vezes (ou uma vez + `runServer`) para simular
   multiplayer.
2. Entre no mundo — sua skin deve estar toda branca.
3. Aperte **P**, escolha uma parte do corpo (ou deixe "Cabeça" selecionado),
   escolha uma cor no quadrado/barra ou digite um hex, e clique em **Aplicar
   na Parte** ou **Preencher Tudo**.
4. No outro cliente conectado, a mudança deve aparecer quase instantaneamente.
5. Olhe para um bloco de grama e aperte **G** — sua skin deve virar verde na
   hora, com uma mensagem na actionbar mostrando o hex capturado.
6. Teste **Auto-Camuflagem** parado perto de blocos variados, e depois ative
   **Mimetismo** e ande pelo mapa observando a skin reagir ao ambiente.

---

## 7. Publicando no GitHub

O projeto já está com `git init` feito e o primeiro commit pronto (veja o
histórico com `git log`). Antes de subir, vale personalizar 3 coisas que
ficaram com placeholder:

| Arquivo | Campo | Onde editar |
|---|---|---|
| `LICENSE` | nome do autor | linha `Copyright (c) 2026 Seu Nome Aqui` |
| `src/main/resources/META-INF/mods.toml` | `authors`, `issueTrackerURL` | troque `"Você"` e `"https://example.com"` pelo seu nome e pela URL real do repositório (ex.: `https://github.com/seu-usuario/skincamo/issues`) |
| `README.md` | links/nome | se for renomear o mod, ajuste aqui também |

Depois disso, é só criar o repositório vazio no GitHub e apontar o remoto:

```bash
cd skincamo

# se ainda não gerou o gradle-wrapper.jar, faça isso primeiro (ver seção 1
# e gradle/wrapper/LEIA-ME_gradle-wrapper-jar.txt), depois:
git add -A
git commit -m "Adiciona gradle-wrapper.jar"

git remote add origin https://github.com/SEU_USUARIO/skincamo.git
git branch -M main
git push -u origin main
```

Se preferir começar o histórico do zero (em vez de manter o commit feito
neste ambiente), basta `rm -rf .git && git init` antes do primeiro `git add`.

O repositório já inclui `.github/workflows/build.yml`: a cada push, o GitHub
instala o JDK 17 e o Gradle 8.1.1 diretamente (via `gradle/actions/setup-gradle`,
**sem depender do `gradle-wrapper.jar`** — por isso ele funciona mesmo antes
de você gerar esse arquivo localmente), compila o mod do zero e disponibiliza
o `.jar` gerado como artefato do workflow. Como o GitHub Actions tem acesso à
internet (diferente de onde este projeto foi montado), esse primeiro build na
nuvem é o jeito mais rápido de confirmar se tudo compila — e, se o mixin
precisar de ajuste (seção 3), o erro vai aparecer claramente no log desse job.

Build local com `./gradlew` continua precisando do `gradle-wrapper.jar` (veja
a seção 1 e o `LEIA-ME` na pasta `gradle/wrapper/`) — isso é só pra rodar o
jogo (`runClient`) na sua máquina; não afeta o CI.


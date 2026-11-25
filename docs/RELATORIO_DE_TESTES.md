# Relatório de testes: versão 1.3.1

**Data:** 25/11/2026
**Versão:** 1.3.2 (versionName) · versionCode 6
**Ambiente:** Windows 11, Android Studio, JDK 21, Gradle 9.9.2 (pelo `gradlew`), Android Gradle Plugin 9.5.3, Kotlin embutido no AGP, compileSdk 37, targetSdk 37.

## 1. Resumo

| Área | Resultado |
|---|---|
| Testes unitários | 15 de 15 passaram |
| Compilação do Kotlin | Sem erros e sem avisos |
| Lint (`lintDebug`) | 0 erros, 0 avisos |
| Build de depuração (`assembleDebug`) | Passou |
| Testes manuais no celular | **Não executados nesta versão** (ver seção 5) |

## 2. Testes unitários

Rodados com `./gradlew :app:testDebugUnitTest`.

| Classe de teste | Testes | Passaram | Falharam |
|---|---|---|---|
| `AvaliadorQRTest` | 15 | 15 | 0 |

Os casos estão descritos em `docs/ROTEIRO_DE_TESTES.md`, seção 1.

## 3. Qualidade do código

- **Avisos do compilador:** o aviso de `announceForAccessibility` foi substituído por envio de evento ao leitor de tela. O envio de anúncio ainda usa um tipo de evento marcado como obsoleto pelo Android; o aviso está suprimido de propósito, com comentário no código, porque é a forma que faz o leitor ler o aviso.
- **Foco forçado:** o movimento de foco para o aviso de status também está suprimido de propósito, com comentário. É o que faz o TalkBack ler o texto novo quando a pessoa está parada.
- **Preferências:** a edição usa a extensão KTX. A preferência do microfone foi removida, porque a tela não pede mais essa permissão.
- **Orientação da tela:** as travas de tela em retrato foram retiradas. As telas têm rolagem, então funcionam em qualquer orientação.
- **Cabeçalho:** o ícone do título virou um desenho ao lado do texto, o que deixou o cabeçalho mais simples para o leitor de tela. O ícone é decoração, e não é lido.
- **Fundo:** removi o fundo repetido das telas, porque o tema já pinta o fundo.

## 4. Dependências e versões

| Item | Antes (1.3) | Agora (1.3.1) |
|---|---|---|
| Gradle | 9.9.2 | 9.9.2 |
| Android Gradle Plugin | 9.5.3 | 9.5.3 |
| Kotlin | embutido no AGP | embutido no AGP (plugin separado removido) |
| compileSdk | 37 | 37 |
| targetSdk | 34 | **37** |
| core-ktx | 1.19.1 | 1.19.1 |
| appcompat | 1.8.0 | 1.8.0 |
| material | 1.14.0 | 1.14.0 |
| CameraX | 1.7.0 | 1.7.0 |
| ML Kit barcode | 17.3.0 | 17.3.0 |
| JUnit (testes) | 4.13.2 | 4.13.2 |

**Atenção sobre o targetSdk 37:** esta mudança faz o app seguir o comportamento do Android mais novo, incluindo tela de borda a borda. Ela foi feita para cumprir o aviso do lint e as exigências da Play Store. Precisa ser testada no celular (seção 5).

## 4.1 Mudança de comportamento na 1.3.2

- **Avisos dinâmicos:** deixam de mover o foco. Cada aviso é falado pela voz do app (se ligada) ou anunciado ao leitor de tela, sem tirar o foco de quem está arrastando a tela.
- **Foco inicial:** ao abrir, o foco vai para o título, uma vez.
- **Pendência:** esta mudança precisa ser confirmada no Samsung A55 com TalkBack, sem arrastar a tela.

## 5. Testes manuais

**Não foram executados nesta versão.** O roteiro completo está em `docs/ROTEIRO_DE_TESTES.md`, seção 2.

Itens em aberto, prioridade alta:

| Item | Situação |
|---|---|
| Avisos dinâmicos lidos sem arrastar a tela (TalkBack, Samsung A55) | Em aberto. Mudança feita na 1.3.2 (seção 4.1). |
| Layout com targetSdk 37 (tela de borda a borda, cabeçalho e rolagem) | A testar. |
| Botão "Configurações do aplicativo" alcançado pelo arrasto | A testar. |
| Foco inicial no título | Corrigido na 1.3.2 (seção 4.1). A testar. |

## 6. Histórico desta correção

- Patch do colega aplicado na 1.3 (backup, `VibratorManager`, ícone monocromático, limpeza de textos).
- Esta versão corrige os avisos que restavam do compilador e do lint, atualiza o targetSdk e adiciona a licença MIT.

## 7. Conclusão

A versão 1.3.1 compila sem erros nem avisos, passa nos 15 testes unitários e não tem avisos de lint. O funcionamento no celular ainda precisa ser confirmado com o roteiro manual, principalmente a leitura automática dos avisos.

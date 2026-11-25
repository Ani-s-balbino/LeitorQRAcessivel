# Relatório de testes: versão 1.3

**Data:** 25/11/2026
**Branch:** `revisao-amigo` (mesclada em `main`)
**Tag:** `v1.3` (o commit exato é o da própria tag)
**Ambiente:** Windows 11, Android Studio com SDK 37, Gradle 9.9.2, AGP 9.5.3, Kotlin embutido no AGP.

## 1. Resumo

| Área | Resultado |
|---|---|
| Testes unitários | 15 de 15 passaram |
| Build de depuração (`assembleDebug`) | Passou |
| Lint (`lintDebug`) | 0 erros, 26 avisos |
| Testes manuais no celular | **Não executados nesta versão** (ver seção 4) |

## 2. Testes unitários

Rodados com `./gradlew :app:testDebugUnitTest`.

| Classe de teste | Testes | Passaram | Falharam |
|---|---|---|---|
| `AvaliadorQRTest` | 15 | 15 | 0 |

Os casos estão descritos em `docs/ROTEIRO_DE_TESTES.md`, seção 1.

## 3. Build e lint

- Build de depuração: **BUILD SUCCESSFUL**.
- Lint: **0 erros, 26 avisos**. Por categoria:

| Categoria | Avisos | Observação |
|---|---|---|
| UseKtx | 11 | Sugestão de estilo (usar extensões do Kotlin). Não afeta o funcionamento. |
| Overdraw | 4 | Pintura repetida de fundo. Ajuste de desempenho, sem impacto visível. |
| LockedOrientationActivity | 4 | As telas estão travadas em retrato. É uma escolha de projeto. |
| DiscouragedApi | 4 | Uso de APIs de acessibilidade ou de sistema que o Google desencoraja. Ver seção 5. |
| UseCompoundDrawables | 1 | Sugestão de layout. |
| OldTargetApi | 1 | `targetSdk` está em 34. Ver seção 5. |
| AccessibilityFocus | 1 | Foco forçado no aviso de status. É a forma usada para que o TalkBack leia o aviso, e está em revisão. |

## 4. Testes manuais

**Estes testes não foram executados nesta versão.** O roteiro completo está em `docs/ROTEIRO_DE_TESTES.md`, seção 2.

Relatos anteriores de testers, que motivaram ajustes desta versão:

| Item | Relato | Situação nesta versão |
|---|---|---|
| Foco inicial | Ao abrir, o foco ia para o cartão de permissão, em vez do título | Corrigido na 1.3 (aviso inicial não move mais o foco). Não confirmado no celular. |
| Textos grudados | Título e parágrafos eram lidos como um único item | Corrigido na 1.3 (cada texto é focável). Não confirmado no celular. |
| Avisos sem leitura | Avisos dinâmicos não eram lidos sem arrastar a tela | Em aberto. Testar no Samsung A55 com TalkBack. |
| Botão de configurações | Não era alcançado quando havia resultado na tela | Corrigido na 1.3 (rolagem até o botão). Não confirmado no celular. |
| Lanterna | Não era alcançada pelo leitor | Corrigido na 1.3. Não confirmado no celular. |
| Voz do aplicativo | Desligada por padrão, e o TalkBack não lia os avisos | Ligada por padrão na 1.2 e na 1.3. |

## 5. Dependências e versões

Atualizadas nesta versão:

| Item | Antes | Depois |
|---|---|---|
| Gradle | 8.9 | 9.9.2 |
| Android Gradle Plugin | 8.5.2 | 9.5.3 |
| Kotlin | 1.9.24 (plugin separado) | embutido no AGP 9 |
| compileSdk | 34 | 37 |
| targetSdk | 34 | 34 (mantido) |
| core-ktx | 1.13.1 | 1.19.1 |
| appcompat | 1.7.0 | 1.8.0 |
| material | 1.12.0 | 1.14.0 |
| CameraX | 1.3.4 | 1.7.0 |
| ML Kit barcode | 17.3.0 | 17.3.0 (já era a última) |
| JUnit (testes) | — | 4.13.2 |

**Pendência:** `targetSdk` continua em 34. Para publicar na Play Store, o Google exige uma versão mais nova. Subir essa versão muda o comportamento do app em Android novos, por exemplo a tela de borda a borda. Deve ser testado antes.

## 6. Correções revisadas do amigo

O patch `correcoes-LeitorQRAcessivel.patch` foi revisado e aplicado. Ele faz:

- Regras de backup do app (`regras_extracao_dados.xml`), que excluem os dados do app do backup.
- Troca do uso de `Vibrator` por `VibratorManager` a partir do Android 12, com a versão antiga mantida para versões anteriores.
- Ícone monocromático para o launcher.
- Remoção de textos que não eram usados.
- Ajuste de `?.` em objetos que nunca são nulos.

## 7. Conclusão

A versão 1.3 compila, passa nos testes unitários e tem lint sem erros. O funcionamento no celular **ainda precisa ser confirmado** com o roteiro manual, especialmente a leitura automática dos avisos com TalkBack no Samsung A55.

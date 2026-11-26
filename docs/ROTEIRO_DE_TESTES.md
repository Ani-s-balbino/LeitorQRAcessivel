# Roteiro de testes

Este roteiro tem duas partes: os **testes unitários**, que o computador roda sozinho, e os **testes manuais**, feitos com o celular e o leitor de tela.

## 1. Testes unitários (computador)

Os testes ficam em `app/src/test/java/br/com/leitorqr/`. Eles conferem as regras de orientação da busca (`AvaliadorQR`), sem precisar de celular.

Para rodar, na pasta do projeto:

```bash
./gradlew :app:testDebugUnitTest
```

No Windows, use `gradlew.bat :app:testDebugUnitTest`.

O resultado aparece no terminal como `BUILD SUCCESSFUL`. Os detalhes ficam em `app/build/reports/tests/testDebugUnitTest/index.html`.

Casos cobertos:

| Caso | Esperado |
|---|---|
| QR code muito perto | Aviso para afastar, com tom de erro |
| QR code à esquerda, à direita, acima ou abaixo | Aviso com a direção certa |
| Direções combinadas | Uma frase só, como "para a direita e para cima" |
| QR code muito fora do centro | Nível de ajuste 0 |
| Dentro da tolerância do centro | Sem aviso de direção |
| QR code muito longe, um pouco longe, um pouco perto | Aviso de distância certo |
| Celular inclinado | Aviso de inclinação |
| Enquadramento perfeito | "QR code bem enquadrado. Segure parado." |
| Cálculo de inclinação | Zero de frente, maior quando torto, e sem divisão por zero |

## 2. Testes manuais (celular e leitor de tela)

Teste com a versão instalada no celular. Marque cada item como **OK**, **FALHOU** ou **NÃO TESTADO**, e anote o modelo do celular e a versão do Android.

### 2.1 Instalação e abertura

- [ ] O app instala sem erro pelo script `scripts/instalar-android.ps1`.
- [ ] O app abre sem travar.
- [ ] Ao abrir, o foco do leitor de tela cai no título "Leitor QR Acessível".
- [ ] O título é lido como título (cabeçalho).

### 2.2 Ordem de foco e leitura

- [ ] Arrastar para a direita percorre os elementos de cima para baixo, sem pular nenhum.
- [ ] Arrastar para a esquerda volta pelos elementos na ordem inversa.
- [ ] Cada parágrafo da permissão tem foco próprio.
- [ ] O texto de status aparece uma vez só, sem repetição.
- [ ] Os botões são lidos com o nome certo, incluindo "Configurações do aplicativo".

### 2.3 Permissão da câmera

- [ ] Sem permissão, o botão "Permitir uso da câmera" aparece e pede a permissão.
- [ ] Com a permissão negada, o primeiro parágrafo explica como liberar nas configurações.
- [ ] Depois de liberada, a câmera abre e a busca começa.

### 2.4 Busca guiada

- [ ] O app avisa "Procurando QR code" e os sinais de busca (bips e/ou vibração) acontecem.
- [ ] Os avisos de direção aparecem (esquerda, direita, cima, baixo) quando o QR code está fora do centro.
- [ ] Os avisos de distância aparecem (longe e perto).
- [ ] Com o QR code bem enquadrado, o app diz "Segure parado" e depois lê o conteúdo.
- [ ] Um QR code danificado gera o aviso de que não foi possível ler.
- [ ] Em ambiente escuro, o app sugere a lanterna.

### 2.5 Avisos dinâmicos

- [ ] Quando a pessoa está parada, o aviso novo recebe o foco do leitor de tela.
- [ ] Quando a pessoa está arrastando a tela, o aviso novo é lido sem tirar o foco dela.
- [ ] Os avisos não se atropelam: há uma pausa entre um e outro.
- [ ] Com o TalkBack ligado, os avisos são lidos sozinhos (sem arrastar a tela). Confirmado na versão 1.4.0.
- [ ] Ajuste pendente: o aviso "QR code lido" demora porque os avisos anteriores ficam na fila de leitura.

### 2.6 Parar e retomar

- [ ] "Parar busca" para os bips, a vibração e a busca.
- [ ] "Retomar busca" volta a buscar.
- [ ] Sair do app (voltar ou trocar de app) para a busca. Ao voltar, a busca só retoma quando a pessoa tocar em "Retomar busca".

### 2.7 Resultado da leitura

- [ ] Um link mostra "Abrir QR code" e abre o site.
- [ ] Um texto mostra "Copiar conteúdo" e "Compartilhar QR code".
- [ ] "Ouvir novamente" repete o conteúdo.
- [ ] "Escanear outro QR code" recomeça a busca.

### 2.8 Configurações do aplicativo

- [ ] O botão "Configurações do aplicativo" é alcançado pelo arrasto, com ou sem resultado na tela.
- [ ] A opção de feedback (nenhum, vibração, bips ou vibração e bips) funciona e fica salva ao fechar o app.
- [ ] Com "Nenhum" selecionado, não há bips nem vibração durante a busca nem ao ler o QR code.
- [ ] "Usar a voz do aplicativo" vem desligada, porque o TalkBack já lê os avisos.
- [ ] Ligar a voz do aplicativo faz ela falar os avisos; desligar faz parar.
- [ ] O aviso sobre o TalkBack aparece quando o TalkBack está ligado.
- [ ] "Permitir câmera" e "Abrir configurações do aplicativo para liberar" funcionam.
- [ ] "Enviar feedback por e-mail" abre o aplicativo de e-mail, com o endereço já preenchido.
- [ ] O endereço de e-mail não aparece escrito na tela.

### 2.9 Privacidade, termos e dicas

- [ ] "Política de privacidade e termos de uso" abre, com o botão Voltar no início.
- [ ] Cada parágrafo e cada item dos termos tem foco próprio.
- [ ] "Dicas para captura de QR code" abre, com o botão Voltar no início.
- [ ] Cada dica tem foco próprio.

### 2.10 Lanterna

- [ ] O botão "Lanterna" aparece e é alcançado pelo leitor de tela.
- [ ] Ligar e desligar a lanterna funciona, e o estado muda no botão e no aviso.

### 2.11 Relatório

Depois de rodar os testes, preencha `docs/RELATORIO_DE_TESTES.md` com o resultado de cada item acima.

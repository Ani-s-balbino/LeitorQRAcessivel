# Leitor QR Acessível

App Android nativo (Kotlin + CameraX + ML Kit) que guia pessoas cegas até um QR code na tela,
com voz em português e bips ao vivo.

Abrir no Android Studio: menu File > Open > escolher a pasta LeitorQRAcessivel.
Rodar no celular (depuração USB ligada): Shift+F10.
Calibragem: constantes no começo de MainActivity.kt (bloco "Ajustes").

## Instalação no Android pelo terminal

Para testar a versão de teste, sem precisar do Android Studio:

1. Ligue o celular no computador pelo cabo USB e ative a depuração USB (Configurações > Opções do desenvolvedor).
2. No PowerShell, dentro da pasta do projeto, rode:

```
powershell -ExecutionPolicy Bypass -File .\scripts\instalar-android.ps1
```

O script baixa o APK da release mais recente, verifica o celular e instala, mostrando cada passo em frases simples.
Para usar um APK que você já baixou: `-ApkLocal "C:\caminho\arquivo.apk"`.

## Como compilar (linha de comando)

O projeto traz o Gradle wrapper (`gradlew`), então não precisa instalar o Gradle. Você precisa do JDK 17 ou superior e do Android SDK (o Android Studio instala os dois).

```bash
./gradlew :app:testDebugUnitTest   # testes unitários
./gradlew :app:lintDebug           # análise de código
./gradlew :app:assembleDebug       # gera o APK de depuração em app/build/outputs/apk/debug/
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

Documentação de qualidade: `docs/ROTEIRO_DE_TESTES.md`, `docs/RELATORIO_DE_TESTES.md` e `docs/VERSIONAMENTO.md`.

## Licença

Este projeto é distribuído sob a [GNU GPLv3](LICENSE). Você pode usar, estudar, modificar e redistribuir o código. Se distribuir uma versão modificada, ela também precisa ser publicada sob a GPLv3, com o código-fonte disponível.

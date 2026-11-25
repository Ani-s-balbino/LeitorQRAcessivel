# Versionamento

## Regra de números

O app usa três informações de versão no `app/build.gradle.kts`:

- **versionName**: o número que a pessoa vê, no formato `MAIOR.MENOR.CORRECAO`.
  - MAIOR: mudança grande (por exemplo, a primeira versão pública).
  - MENOR: funções novas ou mudanças visíveis para a pessoa.
  - CORRECAO: correções sem função nova.
- **versionCode**: número inteiro que sempre aumenta a cada versão publicada. O Android usa esse número para saber se a versão é mais nova.

## Tags e releases no GitHub

- Cada versão publicada recebe uma tag com o mesmo número: `v1.3`.
- Cada tag tem uma release com o APK de teste anexado.
- Versões para teste são marcadas como **pré-release** na release.

## Histórico

| Versão | versionName | versionCode | Tag | Destaques |
|---|---|---|---|---|
| 1.0 | 1.0 | 1 | v1.0 | Primeira versão do projeto |
| 1.1 | 1.0 | 1 | v1.1 | Avisos por voz, configurações, dicas, parar e retomar |
| 1.2 | 1.0 | 1 | v1.2 | Voz do aplicativo ligada por padrão; script de instalação |
| 1.3 | 1.3 | 4 | v1.3 | Correções de revisão, testes unitários, dependências e Gradle atualizados, roteiro e relatório de testes |
| 1.3.1 | 1.3.1 | 5 | v1.3.1 | Sem avisos de compilador e de lint, targetSdk 37, travas de orientação retiradas, licença MIT |

As versões 1.1 e 1.2 foram publicadas sem alterar o `versionName` e o `versionCode`. A partir da 1.3, os números seguem a regra acima.

## Como publicar uma nova versão

1. Aumente `versionName` e `versionCode` em `app/build.gradle.kts`.
2. Rode os testes unitários: `./gradlew :app:testDebugUnitTest`.
3. Rode o build: `./gradlew :app:assembleDebug`.
4. Preencha o relatório de testes em `docs/RELATORIO_DE_TESTES.md`.
5. Commit, tag e push:

```bash
git add -A
git commit -m "Versão X.Y.Z"
git tag -a vX.Y.Z -m "Versão X.Y.Z para testes"
git push origin main vX.Y.Z
```

6. Crie a release com o APK anexado, com nome `LeitorQRAcessivel-vX.Y.Z-teste.apk`, que é o nome que o script de instalação espera.

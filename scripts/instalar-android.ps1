# Instala o Leitor QR Acessível no celular Android, pelo terminal.
#
# Como usar (no PowerShell, com o celular ligado no computador pelo cabo USB):
#   powershell -ExecutionPolicy Bypass -File .\scripts\instalar-android.ps1
#
# Opções:
#   -Versao v1.2          versão da release no GitHub (padrão: v1.2)
#   -ApkLocal "caminho"   usa um APK que você já baixou, sem baixar de novo
#   -SemInstalar          só baixa e verifica o celular, sem instalar (para teste)
#
# O script escreve cada passo em frases simples, sem enfeites, para leitores de tela.

param(
    [string]$Versao = "v1.2",
    [string]$ApkLocal = "",
    [switch]$SemInstalar
)

$ErrorActionPreference = "Stop"
# Garante que os acentos apareçam certos no terminal e para leitores de tela.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$repositorio = "Ani-s-balbino/LeitorQRAcessivel"
$nomeApk = "LeitorQRAcessivel-$Versao-teste.apk"

function Dizer($texto) {
    Write-Output $texto
}

function Parar-Com-Erro($texto) {
    Write-Output ""
    Write-Output "ERRO: $texto"
    Write-Output "A instalação não foi feita."
    exit 1
}

Dizer "Instalação do Leitor QR Acessível para Android, versão $Versao."
Dizer ""

# 1. Encontrar o adb (a ferramenta que conversa com o celular)
Dizer "Passo 1 de 4: procurando a ferramenta adb no computador."
$adb = $null
$emPath = Get-Command adb -ErrorAction SilentlyContinue
if ($emPath) {
    $adb = $emPath.Source
} else {
    $caminhoPadrao = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $caminhoPadrao) { $adb = $caminhoPadrao }
}
if (-not $adb) {
    Parar-Com-Erro ("não encontrei o adb. Instale o Android platform-tools em developer.android.com/tools/releases/platform-tools, " +
        "descompacte a pasta e rode o script de novo. Ou instale o Android Studio, que já traz o adb.")
}
Dizer "Adb encontrado: $adb"
Dizer ""

# 2. Verificar o celular
Dizer "Passo 2 de 4: verificando se o celular está conectado e autorizado."
$linhas = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\S" }
$conectados = @($linhas | Where-Object { $_ -match "\sdevice$" })
$nao_autorizados = @($linhas | Where-Object { $_ -match "unauthorized" })

if ($conectados.Count -eq 0) {
    if ($nao_autorizados.Count -gt 0) {
        Parar-Com-Erro ("o celular está ligado, mas não autorizou o computador. No celular, aceite a janela " +
            "Permitir depuração USB e marque para sempre permitir. Depois rode o script de novo.")
    }
    Parar-Com-Erro ("nenhum celular encontrado. Confira o cabo USB, ligue a depuração USB em " +
        "Configurações > Opções do desenvolvedor, e rode o script de novo.")
}
if ($conectados.Count -gt 1) {
    Parar-Com-Erro ("mais de um celular está conectado. Deixe só o celular de teste conectado e rode o script de novo.")
}
$serie = ($conectados[0] -split "\s+")[0]
Dizer "Celular encontrado. Número de série: $serie"
Dizer ""

# 3. Obter o APK
Dizer "Passo 3 de 4: preparando o arquivo de instalação."
$pastaDownloads = Join-Path $env:USERPROFILE "Downloads"
$arquivo = Join-Path $pastaDownloads $nomeApk

if ($ApkLocal -ne "") {
    if (-not (Test-Path $ApkLocal)) { Parar-Com-Erro "não encontrei o arquivo $ApkLocal." }
    $arquivo = $ApkLocal
    Dizer "Usando o arquivo que você indicou: $arquivo"
} else {
    $url = "https://github.com/$repositorio/releases/download/$Versao/$nomeApk"
    Dizer "Baixando $nomeApk da release $Versao no GitHub."
    Dizer "Destino: $arquivo"
    try {
        Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -TimeoutSec 180
    } catch {
        Parar-Com-Erro ("não consegui baixar o arquivo. Confira sua internet e se a release $Versao existe em " +
            "github.com/$repositorio/releases.")
    }
    $tamanhoMb = [math]::Round((Get-Item $arquivo).Length / 1MB, 1)
    Dizer "Download concluído. Tamanho: $tamanhoMb megabytes."
}
Dizer ""

# 4. Instalar
if ($SemInstalar) {
    Dizer "Passo 4 de 4: instalação pulada, porque você usou -SemInstalar."
    Dizer "Tudo certo até aqui. O arquivo está em $arquivo."
    exit 0
}

Dizer "Passo 4 de 4: instalando no celular. Isso leva alguns segundos."
$saida = & $adb -s $serie install -r "$arquivo" 2>&1 | Out-String
if ($saida -match "Success") {
    Dizer "Instalação concluída com sucesso."
    Dizer ""
    Dizer "Agora abra o aplicativo Leitor QR Acessível no celular."
    Dizer "Se quiser usar a voz do aplicativo, ela já vem ligada. Para desligar, abra Configurações do aplicativo e desmarque Usar a voz do aplicativo."
    Dizer "Lembrete: se o TalkBack estiver ligado, a voz do aplicativo e o TalkBack podem falar juntos."
    exit 0
}

Dizer "O celular respondeu:"
Dizer $saida
Parar-Com-Erro "o celular não aceitou a instalação. Leia a mensagem acima."

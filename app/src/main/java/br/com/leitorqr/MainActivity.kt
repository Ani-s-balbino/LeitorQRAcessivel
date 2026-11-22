package br.com.leitorqr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Size
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import br.com.leitorqr.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Leitor de QR code para pessoas cegas.
 *
 * Fluxo: a câmera traseira analisa cada quadro. Enquanto não acha QR code, dá dicas de busca.
 * Quando acha, avalia posição, distância (tamanho do QR no quadro) e inclinação, e orienta por
 * voz + bips até o QR estar bem enquadrado. Aí confirma com som, vibração e lê o conteúdo.
 */
class MainActivity : AppCompatActivity() {

    // ====== Ajustes (pode mexer para calibrar) ======
    private companion object {
        const val TOLERANCIA_CENTRO = 0.15f      // quanto o QR pode estar fora do centro (fração do quadro)
        const val TAMANHO_MUITO_LONGE = 0.18f    // tamanho do QR / lado menor do quadro
        const val TAMANHO_UM_POUCO_LONGE = 0.30f
        const val TAMANHO_UM_POUCO_PERTO = 0.70f
        const val TAMANHO_MUITO_PERTO = 0.80f
        const val LIMITE_INCLINACAO = 0.18f      // diferença entre lados opostos do QR (perspectiva)
        const val TEMPO_ENQUADRADO_MS = 600L     // tempo que precisa ficar bem enquadrado para concluir
        const val TEMPO_LIMITE_ENQUADRAR_MS = 8000L // se não enquadrar em 8 s, lê mesmo assim
        const val LUZ_MINIMA = 45                // brilho médio (0 a 255) abaixo disso = escuro
        const val MAX_CARACTERES_FALADOS = 300
        const val PAUSA_ENTRE_MENSAGENS_MS = 1500L // pausa entre um aviso e outro (1 a 3 s) para o leitor de tela não se atropelar
        const val REPETIR_MENSAGEM_MS = 5000L      // a mesma orientação só volta a ser anunciada depois disso
        const val NAVEGANDO_RECENTE_MS = 2500L     // se a pessoa moveu o foco há pouco, o aviso não tira o foco dela
        const val JANELA_FOCO_PROPRIO_MS = 500L    // foco que o próprio app moveu (não conta como navegação da pessoa)
        const val DURACAO_PULSO_BUSCA_MS = 3000L   // respiração lenta da moldura da câmera (sem piscar)

        // Esquemas que nunca são abertos: servem para atacar o aparelho, não para o usuário.
        val ESQUEMAS_BLOQUEADOS = setOf(
            "javascript", "intent", "file", "content", "android-app", "data", "vbscript"
        )
        // Atalho de aplicativo: esquema://alguma-coisa (ex.: itauvarejo://home)
        val REGEX_ATALHO = Regex("^([a-zA-Z][a-zA-Z0-9+.\\-]*)://\\S+$")
        val REGEX_SITE_SEM_ESQUEMA = Regex("^www\\.\\S+\\.\\S+$", RegexOption.IGNORE_CASE)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var executorCamera: ExecutorService
    private var camera: Camera? = null
    private var cameraIniciando = false
    private var lanternaLigada = false
    private var acaoAtual: AcaoQr? = null

    // Avisos: uma fila de uma mensagem por vez, com pausa entre elas (ver anunciar)
    private var pendente: String? = null
    private var pendenteUrgente = false
    private var ultimaMensagem = ""
    private var ultimaMensagemTempo = 0L
    private val mostrarProximaMensagem = Runnable { mostrarProxima() }
    private var ultimaNavegacao = 0L     // última vez que a pessoa moveu o foco com o leitor de tela
    private var ultimoFocoProprio = 0L   // última vez que o próprio app moveu o foco
    private var animacaoBusca: ObjectAnimator? = null
    private var buscaPausada = false     // parada pela pessoa (botão) ou porque o app saiu da tela

    // Voz do aplicativo (TTS), só existe se a pessoa ligar nas configurações
    private var tts: TextToSpeech? = null
    private var ttsPronto = false

    // Bips
    private var toneGen: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var intervaloBip = 1500L
    private var tomBip = ToneGenerator.TONE_SUP_PIP

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    // Estado da leitura
    @Volatile
    private var leituraConcluida = false
    private var conteudoLido = ""
    private var ultimaDeteccao = 0L
    private var inicioDeteccao = 0L
    private var inicioEnquadrado = 0L
    private var inicioEscuro = 0L

    private data class Orientacao(
        val fala: String,
        val nivel: Int, // 0 = muito errado, 1 = ajuste médio, 2 = ajuste fino, 3 = perfeito
        val tom: Int = ToneGenerator.TONE_PROP_BEEP
    )

    /** O que dá para fazer com o QR code lido (abrir, e como). */
    private class AcaoQr(
        val descricao: String,            // aparece na tela: "Link da internet", "Planilha do Google"...
        val fala: String,                 // dito em voz alta ao concluir a leitura
        val rotulo: String? = null,       // complemento do botão "Abrir QR code: ..."; null = não há o que abrir
        val intent: Intent? = null,
        val usarSeletor: Boolean = false, // true = o Android pergunta com qual app abrir
        val senhaParaCopiar: String? = null,
        val falarConteudo: Boolean = true
    )

    private val pedirPermissao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            atualizarPermissao(falarOrientacao = true)
        }

    /** Bloqueada = a pessoa já foi pedida, não liberou e o Android não mostra mais a janela de pedido (só pelas configurações). */
    private fun permissaoBloqueada() =
        Preferencias.pediuCamera(this) && !temPermissao() &&
            !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        executorCamera = Executors.newSingleThreadExecutor()
        toneGen = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: RuntimeException) {
            null
        }

        configurarVisual()
        configurarBotoes()
        acompanharNavegacao()
        garantirVisivelAoReceberFoco()
        sincronizarVoz()
        // Ao abrir, o foco do leitor fica no primeiro elemento (o título). Por isso o aviso inicial
        // só muda o texto e não move o foco; a permissão é verificada em onResume.
        binding.textoStatus.text = getString(
            if (temPermissao()) R.string.status_inicial
            else if (permissaoBloqueada()) R.string.permissao_status_bloqueada else R.string.permissao_status_pedir
        )
    }

    // ===================== Avisos e voz =====================

    /**
     * Único caminho para o que o app diz. Cada aviso vai para o texto da tela e recebe o foco
     * do leitor de tela (TalkBack/NVDA), e a voz do app, se estiver ligada nas configurações.
     * Entre um aviso e outro há uma pausa, para o leitor não se atropelar.
     * Um aviso igual ao último só volta depois de alguns segundos.
     */
    private fun anunciar(texto: String, urgente: Boolean = false) {
        val agora = SystemClock.elapsedRealtime()
        if (!urgente && texto == ultimaMensagem && agora - ultimaMensagemTempo < REPETIR_MENSAGEM_MS) return
        // Um aviso importante (ex.: QR code lido) não é trocado por um aviso comum ainda na fila.
        if (!urgente && pendenteUrgente) return
        pendente = texto
        pendenteUrgente = urgente
        val espera = max(0L, PAUSA_ENTRE_MENSAGENS_MS - (agora - ultimaMensagemTempo))
        handler.removeCallbacks(mostrarProximaMensagem)
        handler.postDelayed(mostrarProximaMensagem, espera)
    }

    private fun mostrarProxima() {
        val texto = pendente ?: return
        pendente = null
        pendenteUrgente = false
        val agora = SystemClock.elapsedRealtime()
        ultimaMensagem = texto
        ultimaMensagemTempo = agora

        if (agora - ultimaNavegacao < NAVEGANDO_RECENTE_MS) {
            // A pessoa está percorrendo a tela: o aviso é anunciado pelo TalkBack sem tirar o foco dela.
            ViewCompat.setAccessibilityLiveRegion(binding.textoStatus, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE)
            binding.textoStatus.text = texto
            binding.textoStatus.announceForAccessibility(texto)
        } else {
            // Ela está parada: o foco vai para o aviso novo. Sem região "ao vivo", para não ler duas vezes.
            ViewCompat.setAccessibilityLiveRegion(binding.textoStatus, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE)
            binding.textoStatus.text = texto
            ultimoFocoProprio = agora
            binding.textoStatus.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
        }
        if (ttsPronto) {
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "orientacao")
        }
    }

    /**
     * Registra quando a pessoa move o foco (arrastar para a direita ou esquerda com o TalkBack).
     * Assim um aviso novo não tira o foco no meio da navegação.
     */
    private fun acompanharNavegacao() {
        binding.root.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onRequestSendAccessibilityEvent(host: ViewGroup, child: View, event: AccessibilityEvent): Boolean {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    val agora = SystemClock.elapsedRealtime()
                    if (agora - ultimoFocoProprio > JANELA_FOCO_PROPRIO_MS) ultimaNavegacao = agora
                }
                return super.onRequestSendAccessibilityEvent(host, child, event)
            }
        }
    }

    /**
     * Quando o TalkBack move o foco para um botão que está fora da tela, a tela rola até ele.
     * O TalkBack às vezes não faz essa rolagem sozinho dentro da tela principal.
     */
    private fun garantirVisivelAoReceberFoco() {
        val alvos = listOf(
            binding.botaoPermissao, binding.textoStatus, binding.botaoLanterna, binding.botaoPausarBusca,
            binding.botaoDicas, binding.botaoConfiguracoes, binding.botaoAbrirQr, binding.botaoCompartilhar,
            binding.botaoCopiar, binding.botaoLerNovamente, binding.botaoEscanearOutro
        )
        alvos.forEach { alvo ->
            alvo.accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun sendAccessibilityEvent(host: View, eventType: Int) {
                    super.sendAccessibilityEvent(host, eventType)
                    if (eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                        host.post { host.requestRectangleOnScreen(Rect(0, 0, host.width, host.height), true) }
                    }
                }
            }
        }
    }

    // ===================== Animação da busca =====================

    /** Respiração lenta e discreta da moldura da câmera. Sem piscar, para não afetar quem tem epilepsia ou dislexia. */
    private fun iniciarAnimacaoBusca() {
        if (animacaoBusca != null || reduzirAnimacoes()) return
        animacaoBusca = ObjectAnimator.ofPropertyValuesHolder(
            binding.cartaoCamera,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.02f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.02f)
        ).apply {
            duration = DURACAO_PULSO_BUSCA_MS
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun pararAnimacaoBusca() {
        animacaoBusca?.cancel()
        animacaoBusca = null
        binding.cartaoCamera.scaleX = 1f
        binding.cartaoCamera.scaleY = 1f
    }

    /** Respeita a opção "remover animações" do celular. */
    private fun reduzirAnimacoes() =
        android.provider.Settings.Global.getFloat(
            contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f

    /** Liga ou desliga a voz do app conforme a configuração. Não fala nada ao iniciar. */
    private fun sincronizarVoz() {
        val ligada = Preferencias.vozDoAplicativoLigada(this)
        if (ligada && tts == null) {
            iniciarVoz()
        } else if (!ligada && tts != null) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            ttsPronto = false
        }
    }

    private fun iniciarVoz() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts?.language = Locale.forLanguageTag("pt-BR")
                // Uso "acessibilidade" para a voz sair mesmo com o leitor de tela ativo.
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                ttsPronto = true
            }
        }
    }

    // ===================== Visual =====================

    private fun configurarVisual() {
        val claro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES
        // Ícones da barra de status/navegação escuros no tema claro e claros no tema escuro.
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = claro
            isAppearanceLightNavigationBars = claro
        }
        // Títulos viram "cabeçalhos" para a navegação rápida do TalkBack.
        ViewCompat.setAccessibilityHeading(binding.textoTitulo, true)
    }

    // ===================== Botões =====================

    private fun configurarBotoes() {
        binding.botaoPermissao.setOnClickListener {
            if (permissaoBloqueada()) {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            } else {
                Preferencias.marcarPediuCamera(this)
                pedirPermissao.launch(Manifest.permission.CAMERA)
            }
        }
        binding.botaoLanterna.setOnClickListener { alternarLanterna() }
        binding.botaoLerNovamente.setOnClickListener {
            anunciar(textoParaFalar(conteudoLido), urgente = true)
        }
        binding.botaoCopiar.setOnClickListener { copiarConteudo() }
        binding.botaoAbrirQr.setOnClickListener { abrirQrCode() }
        binding.cartaoResultado.setOnClickListener { abrirQrCode() }
        binding.botaoCompartilhar.setOnClickListener { compartilharConteudo() }
        binding.botaoEscanearOutro.setOnClickListener { recomecar() }
        binding.botaoConfiguracoes.setOnClickListener {
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }
        binding.botaoDicas.setOnClickListener {
            startActivity(Intent(this, DicasActivity::class.java))
        }
        binding.botaoPausarBusca.setOnClickListener {
            if (buscaPausada) retomarBusca() else pausarBusca(avisar = true)
        }
    }

    // ===================== Parar e retomar a busca =====================

    /** Para bips, vibração e animação. Não pede para a câmera parar: retomar fica instantâneo. */
    private fun pausarBusca(avisar: Boolean) {
        buscaPausada = true
        handler.removeCallbacks(loopBip)
        toneGen?.stopTone()
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        pararAnimacaoBusca()
        atualizarBotaoPausa()
        if (avisar) anunciar(getString(R.string.status_busca_parada), urgente = true)
    }

    private fun retomarBusca() {
        buscaPausada = false
        atualizarBotaoPausa()
        iniciarBusca()
        anunciar(getString(R.string.status_busca_retomada), urgente = true)
    }

    private fun atualizarBotaoPausa() {
        binding.botaoPausarBusca.text = getString(
            if (buscaPausada) R.string.botao_retomar_busca else R.string.botao_parar_busca
        )
        binding.botaoPausarBusca.setIconResource(if (buscaPausada) R.drawable.ic_retomar else R.drawable.ic_pausar)
    }

    /** Liga bips e animação, se a busca estiver ativa. */
    private fun iniciarBusca() {
        if (leituraConcluida || buscaPausada || camera == null) return
        iniciarBips()
        iniciarAnimacaoBusca()
    }

    private fun alternarLanterna() {
        val cam = camera
        if (cam == null || !cam.cameraInfo.hasFlashUnit()) {
            anunciar("Este aparelho não tem lanterna disponível.", urgente = true)
            return
        }
        lanternaLigada = !lanternaLigada
        cam.cameraControl.enableTorch(lanternaLigada)
        binding.botaoLanterna.text = getString(
            if (lanternaLigada) R.string.botao_lanterna_ligada else R.string.botao_lanterna_desligada
        )
        anunciar(if (lanternaLigada) "Lanterna ligada" else "Lanterna desligada", urgente = true)
    }

    private fun copiarTexto(texto: String, sensivel: Boolean = false) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR code", texto)
        if (sensivel && Build.VERSION.SDK_INT >= 33) {
            // Pede ao Android para não mostrar o conteúdo na prévia da área de transferência.
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)
    }

    private fun copiarConteudo() {
        copiarTexto(conteudoLido)
        anunciar("Conteúdo copiado.", urgente = true)
    }

    private fun compartilharConteudo() {
        if (conteudoLido.isEmpty()) return
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, conteudoLido)
        }
        try {
            startActivity(Intent.createChooser(envio, getString(R.string.titulo_compartilhar)))
        } catch (e: ActivityNotFoundException) {
            anunciar("Não há aplicativo disponível para compartilhar.", urgente = true)
        }
    }

    /** Abre o QR code lido do jeito certo para o tipo de conteúdo (link, atalho de app, telefone...). */
    private fun abrirQrCode() {
        val acao = acaoAtual ?: return
        val intent = acao.intent ?: return
        acao.senhaParaCopiar?.let { copiarTexto(it, sensivel = true) }
        try {
            if (acao.usarSeletor) {
                // O Android lista os aplicativos instalados que sabem abrir isto, e a pessoa escolhe.
                startActivity(Intent.createChooser(intent, getString(R.string.titulo_abrir_com)))
            } else {
                startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            anunciar("Não encontrei no celular um aplicativo que abra este QR code.", urgente = true)
        } catch (e: SecurityException) {
            anunciar("O Android não permitiu abrir este QR code.", urgente = true)
        }
    }

    // ===================== Tipo de conteúdo =====================

    private fun criarAcao(qr: Barcode): AcaoQr {
        val valor = (qr.rawValue ?: qr.displayValue ?: "").trim()

        // 1) Tipos que o próprio leitor do Google já entende
        when (qr.valueType) {
            Barcode.TYPE_WIFI -> {
                val wifi = qr.wifi
                val rede = wifi?.ssid?.takeIf { it.isNotBlank() }
                val senha = wifi?.password?.takeIf { it.isNotEmpty() }
                return AcaoQr(
                    descricao = "Rede Wi-Fi" + (rede?.let { ": $it" } ?: ""),
                    fala = "É uma rede Wi-Fi" + (rede?.let { " chamada $it" } ?: "") +
                        ". Ao abrir, copio a senha e levo você às configurações de Wi-Fi.",
                    rotulo = "Wi-Fi",
                    intent = Intent(Settings.ACTION_WIFI_SETTINGS),
                    senhaParaCopiar = senha,
                    falarConteudo = false // não lê a senha em voz alta
                )
            }
            Barcode.TYPE_CONTACT_INFO -> {
                val c = qr.contactInfo
                val nome = c?.name?.formattedName?.takeIf { it.isNotBlank() }
                val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
                    nome?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                    c?.organization?.takeIf { it.isNotBlank() }
                        ?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
                    c?.phones?.firstOrNull()?.number?.takeIf { it.isNotBlank() }
                        ?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                    c?.emails?.firstOrNull()?.address?.takeIf { it.isNotBlank() }
                        ?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                }
                return AcaoQr(
                    descricao = "Contato" + (nome?.let { ": $it" } ?: ""),
                    fala = "É um cartão de contato" + (nome?.let { " de $it" } ?: "") + ".",
                    rotulo = "contato",
                    intent = intent,
                    falarConteudo = false
                )
            }
            Barcode.TYPE_EMAIL -> {
                val email = qr.email
                val endereco = email?.address
                if (!endereco.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(endereco))
                        email?.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                        email?.body?.let { putExtra(Intent.EXTRA_TEXT, it) }
                    }
                    return AcaoQr("E-mail para $endereco", "É um e-mail para $endereco.", "e-mail", intent, usarSeletor = true)
                }
            }
            Barcode.TYPE_PHONE -> {
                val numero = qr.phone?.number
                if (!numero.isNullOrBlank()) {
                    // ACTION_DIAL só abre o discador com o número; quem decide ligar é a pessoa.
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(numero)))
                    return AcaoQr("Telefone $numero", "É um número de telefone: $numero.", "telefone", intent)
                }
            }
            Barcode.TYPE_SMS -> {
                val sms = qr.sms
                val numero = sms?.phoneNumber
                if (!numero.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(numero))).apply {
                        sms?.message?.let { putExtra("sms_body", it) }
                    }
                    return AcaoQr("Mensagem de texto para $numero", "É uma mensagem de texto para $numero.", "mensagem", intent, usarSeletor = true)
                }
            }
            Barcode.TYPE_GEO -> {
                val ponto = qr.geoPoint
                if (ponto != null) {
                    val coord = "${ponto.lat},${ponto.lng}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coord?q=$coord"))
                    return AcaoQr("Localização no mapa", "É uma localização no mapa.", "mapa", intent, usarSeletor = true)
                }
            }
        }

        // 2) Endereços: site sem "https://" vira https
        val texto = if (REGEX_SITE_SEM_ESQUEMA.matches(valor)) "https://$valor" else valor
        val esquema = REGEX_ATALHO.matchEntire(texto)?.groupValues?.get(1)?.lowercase()
            ?: return AcaoQr(
                descricao = "Texto",
                fala = "É um texto. Não há o que abrir; você pode copiar ou compartilhar."
            )

        if (esquema in ESQUEMAS_BLOQUEADOS) {
            return AcaoQr(
                descricao = "Endereço bloqueado por segurança",
                fala = "Atenção: este QR code tem um endereço do tipo $esquema, que o aplicativo não abre por segurança."
            )
        }

        val uri = Uri.parse(texto).buildUpon().scheme(esquema).build()
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)

        // 3) Link da internet (e documentos na internet)
        if (esquema == "http" || esquema == "https") {
            val site = uri.host?.lowercase().orEmpty().removePrefix("www.")
            val caminho = uri.path?.lowercase().orEmpty()
            fun termina(vararg ext: String) = ext.any { caminho.endsWith(it) }
            val (descricao, rotulo) = when {
                site == "docs.google.com" && caminho.startsWith("/document") -> "Documento do Google Docs" to "documento"
                site == "docs.google.com" && caminho.startsWith("/spreadsheets") -> "Planilha do Google Planilhas" to "planilha"
                site == "docs.google.com" && caminho.startsWith("/presentation") -> "Apresentação do Google Apresentações" to "apresentação"
                site == "docs.google.com" && caminho.startsWith("/forms") || site == "forms.gle" -> "Formulário do Google" to "formulário"
                site == "drive.google.com" -> "Arquivo do Google Drive" to "arquivo"
                termina(".doc", ".docx", ".odt", ".rtf") -> "Documento de texto" to "documento"
                termina(".xls", ".xlsx", ".csv", ".ods") -> "Planilha" to "planilha"
                termina(".ppt", ".pptx", ".odp") -> "Apresentação de slides" to "apresentação"
                termina(".pdf") -> "Arquivo PDF" to "PDF"
                else -> "Link da internet" to "link"
            }
            val semCriptografia = if (esquema == "http") " Atenção: este endereço não usa conexão segura." else ""
            return AcaoQr(
                descricao = if (site.isNotEmpty()) "$descricao · $site" else descricao,
                fala = "É um link. Tipo: $descricao." + (if (site.isNotEmpty()) " Site: $site." else "") + semCriptografia,
                rotulo = rotulo,
                intent = intent,
                usarSeletor = true // pergunta em qual aplicativo/navegador instalado abrir
            )
        }

        // 4) Atalho para outro aplicativo (itauvarejo://, etc.)
        val app = nomeDoAplicativo(intent)
        return AcaoQr(
            descricao = "Atalho de aplicativo" + (app?.let { ": $it" } ?: " ($esquema)"),
            fala = if (app != null) "É um atalho que abre no aplicativo $app."
            else "É um atalho para outro aplicativo, do tipo $esquema.",
            rotulo = app ?: "aplicativo",
            intent = intent
        )
    }

    /** Nome do aplicativo que vai abrir o atalho. Só aparece para esquemas declarados em <queries> no manifesto. */
    @Suppress("DEPRECATION")
    private fun nomeDoAplicativo(intent: Intent): String? = try {
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .firstOrNull()?.loadLabel(packageManager)?.toString()
    } catch (e: Exception) {
        null
    }

    private fun textoParaFalar(valor: String): String {
        val corpo = if (valor.length > MAX_CARACTERES_FALADOS) {
            valor.take(MAX_CARACTERES_FALADOS) + ". E mais texto."
        } else valor
        return "Conteúdo: $corpo"
    }

    // ===================== Câmera =====================

    private fun temPermissao() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /** Mostra o painel de permissão ou a câmera, conforme a situação atual. */
    private fun atualizarPermissao(falarOrientacao: Boolean = false) {
        if (temPermissao()) {
            binding.cartaoPermissao.visibility = View.GONE
            if (!leituraConcluida) {
                binding.cartaoCamera.visibility = View.VISIBLE
                binding.cartaoStatus.visibility = View.VISIBLE
                binding.botaoLanterna.visibility = View.VISIBLE
            }
            binding.botaoPausarBusca.visibility = if (leituraConcluida) View.GONE else View.VISIBLE
            atualizarBotaoPausa()
            binding.botaoConfiguracoes.visibility = View.VISIBLE
            if (camera == null && !cameraIniciando) iniciarCamera()
            return
        }

        val bloqueada = permissaoBloqueada()
        binding.cartaoPermissao.visibility = View.VISIBLE
        binding.cartaoCamera.visibility = View.GONE
        binding.botaoLanterna.visibility = View.GONE
        binding.botaoPermissao.text = getString(
            if (bloqueada) R.string.permissao_botao_configuracoes else R.string.permissao_botao_permitir
        )
        binding.botaoPermissao.setIconResource(if (bloqueada) R.drawable.ic_abrir else R.drawable.ic_camera)
        // O primeiro parágrafo acompanha o botão: se a câmera está bloqueada, explica como liberar nas configurações.
        binding.textoPermissaoParagrafo1.text = getString(
            if (bloqueada) R.string.permissao_paragrafo_bloqueado else R.string.permissao_paragrafo_1
        )
        val fala = getString(if (bloqueada) R.string.permissao_status_bloqueada else R.string.permissao_status_pedir)
        // O cartão de status continua visível: é nele que o leitor de tela lê o aviso (texto curto, sem repetir a explicação).
        binding.textoStatus.text = fala
        if (falarOrientacao) anunciar(fala, urgente = true)
    }

    // ===================== Câmera =====================

    private fun iniciarCamera() {
        cameraIniciando = true
        buscaPausada = false // primeira vez que a câmera liga: a busca começa
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            val provider = futuro.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Resolução maior ajuda a achar QR codes pequenos ou distantes.
            @Suppress("DEPRECATION")
            val analise = ImageAnalysis.Builder()
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executorCamera) { proxy -> analisar(proxy) } }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analise
            )
            cameraIniciando = false
            iniciarBusca()
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analisar(imageProxy: ImageProxy) {
        val midia = imageProxy.image
        if (midia == null || leituraConcluida || buscaPausada) {
            imageProxy.close()
            return
        }
        val rotacao = imageProxy.imageInfo.rotationDegrees
        val girada = rotacao == 90 || rotacao == 270
        // Tamanho do quadro "em pé", que é o sistema de coordenadas que o ML Kit devolve.
        val largura = if (girada) imageProxy.height else imageProxy.width
        val altura = if (girada) imageProxy.width else imageProxy.height
        val luz = luminanciaMedia(imageProxy)

        scanner.process(InputImage.fromMediaImage(midia, rotacao))
            .addOnSuccessListener { processar(it, largura, altura, luz) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun luminanciaMedia(imagem: ImageProxy): Int {
        val buffer = imagem.planes[0].buffer
        val limite = buffer.limit()
        if (limite == 0) return 255
        var soma = 0L
        var n = 0
        var i = 0
        while (i < limite) {
            soma += buffer.get(i).toInt() and 0xFF
            n++
            i += 97
        }
        return (soma / n).toInt()
    }

    // ===================== Orientação =====================

    private fun processar(codigos: List<Barcode>, largura: Int, altura: Int, luz: Int) {
        if (leituraConcluida) return
        val agora = SystemClock.elapsedRealtime()
        val qr = codigos.firstOrNull { it.boundingBox != null && !(it.rawValue ?: it.displayValue).isNullOrEmpty() }
        if (qr == null) {
            if (codigos.isNotEmpty()) qrQuebrado(agora) else semQrCode(agora, luz)
        } else {
            comQrCode(qr, largura, altura, agora)
        }
    }

    private fun semQrCode(agora: Long, luz: Int) {
        // Tolerância: um quadro sem detecção no meio de vários bons não deve zerar a orientação.
        if (ultimaDeteccao != 0L && agora - ultimaDeteccao < 1000) return

        inicioDeteccao = 0L
        inicioEnquadrado = 0L
        intervaloBip = 1500L
        tomBip = ToneGenerator.TONE_SUP_PIP

        if (luz < LUZ_MINIMA) {
            if (inicioEscuro == 0L) inicioEscuro = agora
            if (agora - inicioEscuro > 2500 && !lanternaLigada) {
                val msg = "Ambiente escuro. Ligue a lanterna no botão Lanterna."
                anunciar(msg)
                return
            }
        } else {
            inicioEscuro = 0L
        }

        val dicas = listOf(
            "Procurando QR code. Mova o celular lentamente.",
            "Nenhum QR code encontrado. Afaste o celular da tela, cerca de trinta centímetros.",
            "Ainda procurando. Aumente o brilho da tela e evite reflexos."
        )
        anunciar(dicas[((agora / 6000) % dicas.size).toInt()])
    }

    /** O leitor achou um desenho de QR code, mas não conseguiu decodificar: geralmente está borrado, cortado ou danificado. */
    private fun qrQuebrado(agora: Long) {
        ultimaDeteccao = agora
        inicioDeteccao = 0L
        inicioEnquadrado = 0L
        anunciar(getString(R.string.erro_qr_quebrado))
    }

    private fun comQrCode(qr: Barcode, largura: Int, altura: Int, agora: Long) {
        ultimaDeteccao = agora
        inicioEscuro = 0L
        if (inicioDeteccao == 0L) inicioDeteccao = agora

        val caixa = qr.boundingBox ?: return

        // Tamanho relativo do QR no quadro: serve de "medidor de distância".
        val tamanho = max(caixa.width(), caixa.height()).toFloat() / min(largura, altura)
        // Posição do centro do QR em relação ao centro do quadro (-0.5 a 0.5).
        val dx = (caixa.exactCenterX() - largura / 2f) / largura
        val dy = (caixa.exactCenterY() - altura / 2f) / altura
        val inclinacao = calcularInclinacao(qr.cornerPoints)

        val o = avaliar(tamanho, dx, dy, inclinacao)

        intervaloBip = when (o.nivel) {
            0 -> 1000L
            1 -> 500L
            2 -> 250L
            else -> 120L
        }
        tomBip = o.tom
        anunciar(o.fala)

        if (o.nivel == 3) {
            if (inicioEnquadrado == 0L) inicioEnquadrado = agora
            if (agora - inicioEnquadrado >= TEMPO_ENQUADRADO_MS) concluir(qr)
        } else {
            inicioEnquadrado = 0L
            if (agora - inicioDeteccao > TEMPO_LIMITE_ENQUADRAR_MS) concluir(qr)
        }
    }

    private fun avaliar(tamanho: Float, dx: Float, dy: Float, inclinacao: Float): Orientacao {
        // 1) Perto demais (QR cortado ou desfocado)
        if (tamanho > TAMANHO_MUITO_PERTO) {
            return Orientacao("QR code muito perto. Afaste o celular.", 0, ToneGenerator.TONE_PROP_NACK)
        }

        // 2) Fora do centro: se o QR aparece à esquerda, o celular deve ir para a esquerda.
        val direcoes = mutableListOf<String>()
        if (dx < -TOLERANCIA_CENTRO) direcoes.add("a esquerda")
        if (dx > TOLERANCIA_CENTRO) direcoes.add("a direita")
        if (dy < -TOLERANCIA_CENTRO) direcoes.add("cima")
        if (dy > TOLERANCIA_CENTRO) direcoes.add("baixo")
        if (direcoes.isNotEmpty()) {
            val muitoFora = abs(dx) > 0.30f || abs(dy) > 0.30f
            return Orientacao(
                "QR code encontrado, fora do centro. Mova o celular para " + direcoes.joinToString(" e para "),
                if (muitoFora) 0 else 1
            )
        }

        // 3) Distância
        if (tamanho < TAMANHO_MUITO_LONGE) {
            return Orientacao("QR code muito longe. Aproxime o celular.", 0)
        }
        if (tamanho < TAMANHO_UM_POUCO_LONGE) {
            return Orientacao("Um pouco longe. Aproxime mais um pouco.", 1)
        }
        if (tamanho > TAMANHO_UM_POUCO_PERTO) {
            return Orientacao("Um pouco perto. Afaste um pouco.", 1)
        }

        // 4) Ângulo da câmera
        if (inclinacao > LIMITE_INCLINACAO) {
            return Orientacao("Celular inclinado. Deixe o celular paralelo à tela.", 2)
        }

        return Orientacao("QR code bem enquadrado. Segure parado.", 3)
    }

    /**
     * Estima a inclinação pela perspectiva: se o celular está torto em relação à tela,
     * lados opostos do QR aparecem com tamanhos diferentes. 0 = de frente, quanto maior, mais torto.
     */
    private fun calcularInclinacao(pontos: Array<Point>?): Float {
        if (pontos == null || pontos.size < 4) return 0f
        fun dist(a: Point, b: Point) = hypot((a.x - b.x).toFloat(), (a.y - b.y).toFloat())
        val topo = dist(pontos[0], pontos[1])
        val direita = dist(pontos[1], pontos[2])
        val base = dist(pontos[2], pontos[3])
        val esquerda = dist(pontos[3], pontos[0])
        val horizontal = abs(topo - base) / max(max(topo, base), 1f)
        val vertical = abs(esquerda - direita) / max(max(esquerda, direita), 1f)
        return max(horizontal, vertical)
    }

    private fun concluir(qr: Barcode) {
        val valor = qr.rawValue ?: qr.displayValue ?: ""
        leituraConcluida = true
        handler.removeCallbacks(loopBip)
        pararAnimacaoBusca()
        conteudoLido = valor

        // A câmera já cumpriu seu papel: apaga a lanterna para não gastar bateria.
        if (lanternaLigada) {
            camera?.cameraControl?.enableTorch(false)
            lanternaLigada = false
            binding.botaoLanterna.text = getString(R.string.botao_lanterna_desligada)
        }

        if (Preferencias.bipsLigados(this)) toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
        if (Preferencias.vibracaoLigada(this)) vibrar()

        val acao = criarAcao(qr)
        acaoAtual = acao
        val podeAbrir = acao.intent != null

        binding.cartaoCamera.visibility = View.GONE
        binding.botaoLanterna.visibility = View.GONE
        binding.botaoPausarBusca.visibility = View.GONE
        binding.textoTipoResultado.text = acao.descricao
        binding.textoResultado.text = valor
        binding.cartaoResultado.visibility = View.VISIBLE
        if (!reduzirAnimacoes()) {
            // Surgimento suave do resultado, sem movimento brusco.
            binding.cartaoResultado.alpha = 0f
            binding.cartaoResultado.animate().alpha(1f).setDuration(400).start()
        }
        binding.cartaoResultado.isFocusable = true
        binding.cartaoResultado.isClickable = podeAbrir
        if (podeAbrir) {
            val rotulo = getString(R.string.acao_abrir_qr_tipo, acao.rotulo)
            // TalkBack: "toque duas vezes para Abrir QR code: link"
            ViewCompat.replaceAccessibilityAction(
                binding.cartaoResultado,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                rotulo,
                null
            )
            binding.botaoAbrirQr.text = rotulo
        }
        binding.botaoAbrirQr.visibility = if (podeAbrir) View.VISIBLE else View.GONE
        binding.botaoCompartilhar.visibility = View.VISIBLE
        binding.botaoCopiar.visibility = View.VISIBLE
        binding.botaoLerNovamente.visibility = View.VISIBLE
        binding.botaoEscanearOutro.visibility = View.VISIBLE

        anunciar(
            "QR code lido. " + acao.fala + (if (acao.falarConteudo) " " + textoParaFalar(valor) else ""),
            urgente = true
        )
    }

    private fun recomecar() {
        leituraConcluida = false
        conteudoLido = ""
        acaoAtual = null
        ultimaDeteccao = 0L
        inicioDeteccao = 0L
        inicioEnquadrado = 0L
        inicioEscuro = 0L
        intervaloBip = 1500L
        tomBip = ToneGenerator.TONE_SUP_PIP

        binding.cartaoResultado.visibility = View.GONE
        binding.cartaoResultado.isClickable = false
        binding.botaoAbrirQr.visibility = View.GONE
        binding.botaoCompartilhar.visibility = View.GONE
        binding.botaoCopiar.visibility = View.GONE
        binding.botaoLerNovamente.visibility = View.GONE
        binding.botaoEscanearOutro.visibility = View.GONE
        buscaPausada = false
        anunciar(getString(R.string.status_inicial), urgente = true)
        atualizarPermissao()
        if (camera != null && temPermissao()) {
            iniciarBusca()
            anunciar("Procurando QR code. Mova o celular lentamente.", urgente = true)
        }
    }

    // ===================== Bips e vibração =====================

    private val loopBip = object : Runnable {
        override fun run() {
            if (leituraConcluida) return
            if (Preferencias.bipsLigados(this@MainActivity)) toneGen?.startTone(tomBip, 120)
            // Na busca, a vibração acompanha o mesmo ritmo dos bips (pulsos curtos).
            if (Preferencias.vibracaoLigada(this@MainActivity)) vibrarPulso()
            handler.postDelayed(this, intervaloBip)
        }
    }

    private fun iniciarBips() {
        handler.removeCallbacks(loopBip)
        handler.post(loopBip)
    }

    @Suppress("DEPRECATION")
    private fun vibrar(duracaoMs: Long = 400) {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(duracaoMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrarPulso() = vibrar(60)

    // ===================== Ciclo de vida =====================

    override fun onPause() {
        super.onPause()
        // Sair da tela (inclusive para Configurações ou Dicas) para tudo que é som e movimento.
        pausarBusca(avisar = false)
        handler.removeCallbacks(mostrarProximaMensagem)
        tts?.stop()
    }

    override fun onStop() {
        super.onStop()
        // Saiu do app (voltar, outro app ou tela desligada): a busca fica parada até a pessoa retomar.
        pausarBusca(avisar = false)
    }

    override fun onResume() {
        super.onResume()
        // Pode ter mudado nas configurações (voz do app ligada/desligada, permissões).
        sincronizarVoz()
        atualizarPermissao()
        if (buscaPausada && !leituraConcluida && camera != null) {
            // Ao voltar, a busca não recomeça sozinha: a pessoa decide quando retomar.
            atualizarBotaoPausa()
            binding.botaoPausarBusca.visibility = View.VISIBLE
        } else {
            iniciarBusca()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loopBip)
        handler.removeCallbacks(mostrarProximaMensagem)
        tts?.shutdown()
        toneGen?.release()
        scanner.close()
        executorCamera.shutdown()
    }
}

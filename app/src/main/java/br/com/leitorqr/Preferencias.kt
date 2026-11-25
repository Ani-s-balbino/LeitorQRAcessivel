package br.com.leitorqr

import android.content.Context

/**
 * Preferências do usuário, guardadas só neste aparelho (SharedPreferences).
 * Nada disto é enviado para fora.
 */
object Preferencias {
    private const val ARQUIVO = "leitor_qr_preferencias"
    private const val CHAVE_VIBRACAO = "feedback_vibracao"
    private const val CHAVE_BIPS = "feedback_bips"
    private const val CHAVE_VOZ_APP = "voz_do_aplicativo"
    private const val CHAVE_PEDIU_CAMERA = "pediu_camera"
    private const val CHAVE_PEDIU_MICROFONE = "pediu_microfone"

    private fun prefs(context: Context) =
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    /** Vibração a cada sinal da busca e ao ler o QR code. Padrão: ligada. */
    fun vibracaoLigada(context: Context) = prefs(context).getBoolean(CHAVE_VIBRACAO, true)
    fun definirVibracao(context: Context, ligada: Boolean) =
        prefs(context).edit().putBoolean(CHAVE_VIBRACAO, ligada).apply()

    /** Bips durante a busca e ao ler o QR code. Padrão: ligados. */
    fun bipsLigados(context: Context) = prefs(context).getBoolean(CHAVE_BIPS, true)
    fun definirBips(context: Context, ligados: Boolean) =
        prefs(context).edit().putBoolean(CHAVE_BIPS, ligados).apply()

    /**
     * Voz do aplicativo (TTS). Padrão: ligada no Android, porque o TalkBack não lê sozinho
     * os textos que mudam na tela (testado no Samsung A55). Se atrapalhar, a pessoa desmarca
     * em Configurações do aplicativo.
     */
    fun vozDoAplicativoLigada(context: Context) = prefs(context).getBoolean(CHAVE_VOZ_APP, true)
    fun definirVozDoAplicativo(context: Context, ligada: Boolean) =
        prefs(context).edit().putBoolean(CHAVE_VOZ_APP, ligada).apply()

    /** Guarda se o pedido de permissão já foi feito, para saber se a permissão foi bloqueada. */
    fun pediuCamera(context: Context) = prefs(context).getBoolean(CHAVE_PEDIU_CAMERA, false)
    fun marcarPediuCamera(context: Context) =
        prefs(context).edit().putBoolean(CHAVE_PEDIU_CAMERA, true).apply()

    fun pediuMicrofone(context: Context) = prefs(context).getBoolean(CHAVE_PEDIU_MICROFONE, false)
    fun marcarPediuMicrofone(context: Context) =
        prefs(context).edit().putBoolean(CHAVE_PEDIU_MICROFONE, true).apply()
}

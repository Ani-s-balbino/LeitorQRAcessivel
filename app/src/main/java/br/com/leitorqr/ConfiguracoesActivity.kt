/*
 * Copyright (C) 2026 Ani-s-balbino
 *
 * Este arquivo é parte do Leitor QR Acessível.
 *
 * Leitor QR Acessível é software livre: você pode redistribuí-lo e/ou modificá-lo
 * sob os termos da GNU General Public License versão 3, publicada pela
 * Free Software Foundation. Este programa é distribuído na esperança de que seja
 * útil, mas SEM NENHUMA GARANTIA. Veja o arquivo LICENSE para mais detalhes.
 */
package br.com.leitorqr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import br.com.leitorqr.databinding.ActivityConfiguracoesBinding
import com.google.android.material.button.MaterialButton

/**
 * Configurações do aplicativo: feedback durante a busca (vibração, bips ou os dois), voz do aplicativo,
 * permissão da câmera, política de privacidade e contato para feedback.
 */
class ConfiguracoesActivity : AppCompatActivity() {

    private companion object {
        // Usado só para abrir o app de e-mail. O endereço nunca aparece na tela.
        const val EMAIL_FEEDBACK = "aniclersb@gmail.com"
    }

    private lateinit var binding: ActivityConfiguracoesBinding

    private val pedirCamera: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { atualizarPermissoes() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracoesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val claro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = claro
            isAppearanceLightNavigationBars = claro
        }

        // Títulos da tela e das seções são cabeçalhos para o leitor de tela.
        listOf(
            binding.textoTituloConfiguracoes,
            binding.tituloFeedback,
            binding.tituloVoz,
            binding.tituloPermissoes,
            binding.tituloAjuda
        ).forEach { ViewCompat.setAccessibilityHeading(it, true) }

        configurarFeedback()
        configurarVoz()

        binding.botaoCameraPermissao.setOnClickListener {
            pedirOuAbrirConfiguracoes(Manifest.permission.CAMERA, Preferencias.pediuCamera(this), pedirCamera) {
                Preferencias.marcarPediuCamera(this)
            }
        }

        binding.botaoPolitica.setOnClickListener {
            startActivity(Intent(this, PoliticaActivity::class.java))
        }
        binding.botaoFeedback.setOnClickListener { enviarFeedback() }
        binding.botaoVoltar.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        atualizarPermissoes()
        atualizarTalkback()
    }

    // ===================== Feedback durante a busca =====================

    private fun configurarFeedback() {
        val vibra = Preferencias.vibracaoLigada(this)
        val bips = Preferencias.bipsLigados(this)
        binding.grupoFeedback.check(
            when {
                vibra && bips -> binding.radioAmbos.id
                vibra -> binding.radioVibracao.id
                else -> binding.radioBips.id
            }
        )
        binding.grupoFeedback.setOnCheckedChangeListener { _, id ->
            // Sempre fica pelo menos um tipo de aviso ligado: a escolha é entre os três.
            val (vibracao, bipsLigados) = when (id) {
                binding.radioVibracao.id -> true to false
                binding.radioBips.id -> false to true
                else -> true to true
            }
            Preferencias.definirVibracao(this, vibracao)
            Preferencias.definirBips(this, bipsLigados)
        }
    }

    // ===================== Voz do aplicativo (TTS) =====================

    private fun configurarVoz() {
        binding.switchVoz.isChecked = Preferencias.vozDoAplicativoLigada(this)
        binding.switchVoz.setOnCheckedChangeListener { _, ligada ->
            Preferencias.definirVozDoAplicativo(this, ligada)
            atualizarTalkback()
        }
    }

    /** Avisa se o TalkBack está ligado, porque ele e a voz do app não devem falar juntos. */
    private fun atualizarTalkback() {
        val gerenciador = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        binding.textoTalkback.text = getString(
            if (gerenciador.isTouchExplorationEnabled) R.string.config_talkback_atencao
            else R.string.config_talkback_desligado
        )
    }

    // ===================== Permissão da câmera =====================

    private fun atualizarPermissoes() {
        val concedida = permissaoConcedida(Manifest.permission.CAMERA)
        binding.textoCamera.text = getString(
            if (concedida) R.string.config_camera_permitida else R.string.config_camera_negada
        )
        // Se já está liberada, não há o que tocar.
        binding.botaoCameraPermissao.visibility = if (concedida) View.GONE else View.VISIBLE
        binding.botaoCameraPermissao.text = getString(
            if (estaBloqueada(Manifest.permission.CAMERA, Preferencias.pediuCamera(this)))
                R.string.config_botao_configuracoes_permissao
            else R.string.config_botao_permitir_camera
        )
    }

    private fun permissaoConcedida(permissao: String) =
        ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED

    /** Bloqueada = já foi pedida, não está liberada e o Android não mostra mais a janela de pedido. */
    private fun estaBloqueada(permissao: String, pediu: Boolean) =
        pediu && !permissaoConcedida(permissao) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(this, permissao)

    private fun pedirOuAbrirConfiguracoes(
        permissao: String,
        pediu: Boolean,
        lancador: ActivityResultLauncher<String>,
        marcarPedido: () -> Unit
    ) {
        if (estaBloqueada(permissao, pediu)) {
            // O Android não mostra mais a janela: a liberação só é possível nas configurações do app.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        } else {
            marcarPedido()
            lancador.launch(permissao)
        }
    }

    // ===================== Feedback =====================

    private fun enviarFeedback() {
        // O endereço fica só dentro do aplicativo de e-mail; não é mostrado na tela.
        val envio = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_FEEDBACK))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.config_feedback_assunto))
        }
        try {
            startActivity(envio)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.config_sem_app_email, Toast.LENGTH_LONG).show()
        }
    }
}

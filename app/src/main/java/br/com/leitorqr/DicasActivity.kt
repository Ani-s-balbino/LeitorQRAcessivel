package br.com.leitorqr

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import br.com.leitorqr.databinding.ActivityDicasBinding

/** Dicas para o usuário encontrar e ler melhor um QR code (papel, tela, distância, luz). */
class DicasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDicasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDicasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val claro = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = claro
            isAppearanceLightNavigationBars = claro
        }

        // Título da tela e títulos das seções são cabeçalhos para o leitor de tela.
        listOf(
            binding.tituloDicas,
            binding.cabecalhoPapel,
            binding.cabecalhoComputador,
            binding.cabecalhoDistancia,
            binding.cabecalhoLuz,
            binding.cabecalhoNaoLeu
        ).forEach { ViewCompat.setAccessibilityHeading(it, true) }

        binding.botaoVoltarDicas.setOnClickListener { finish() }
    }
}

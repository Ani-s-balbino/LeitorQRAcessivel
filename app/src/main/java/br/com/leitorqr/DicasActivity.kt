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

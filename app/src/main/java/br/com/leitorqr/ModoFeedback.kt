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

/**
 * Como o app avisa durante a busca. São as quatro opções da tela de configurações.
 * Sem depender do Android, para os testes unitários.
 */
enum class ModoFeedback(val vibra: Boolean, val tocaBip: Boolean) {
    NENHUM(vibra = false, tocaBip = false),
    VIBRACAO(vibra = true, tocaBip = false),
    BIPS(vibra = false, tocaBip = true),
    VIBRACAO_E_BIPS(vibra = true, tocaBip = true);

    companion object {
        /** Modo a partir das duas preferências guardadas. */
        fun de(vibracao: Boolean, bips: Boolean): ModoFeedback = when {
            vibracao && bips -> VIBRACAO_E_BIPS
            vibracao -> VIBRACAO
            bips -> BIPS
            else -> NENHUM
        }
    }
}

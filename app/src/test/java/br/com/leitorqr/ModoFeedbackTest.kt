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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModoFeedbackTest {

    @Test
    fun semVibracaoESemBipsEhNenhum() {
        assertEquals(ModoFeedback.NENHUM, ModoFeedback.de(vibracao = false, bips = false))
    }

    @Test
    fun soVibracao() {
        assertEquals(ModoFeedback.VIBRACAO, ModoFeedback.de(vibracao = true, bips = false))
    }

    @Test
    fun soBips() {
        assertEquals(ModoFeedback.BIPS, ModoFeedback.de(vibracao = false, bips = true))
    }

    @Test
    fun vibracaoEBips() {
        assertEquals(ModoFeedback.VIBRACAO_E_BIPS, ModoFeedback.de(vibracao = true, bips = true))
    }

    @Test
    fun nenhumNaoVibraNemTocaBip() {
        assertFalse(ModoFeedback.NENHUM.vibra)
        assertFalse(ModoFeedback.NENHUM.tocaBip)
    }

    @Test
    fun vibracaoEBipsLigamOsDois() {
        assertTrue(ModoFeedback.VIBRACAO_E_BIPS.vibra)
        assertTrue(ModoFeedback.VIBRACAO_E_BIPS.tocaBip)
    }

    @Test
    fun cadaModoVoltaAsMesmasPreferencias() {
        for (modo in ModoFeedback.values()) {
            assertEquals(modo, ModoFeedback.de(modo.vibra, modo.tocaBip))
        }
    }
}

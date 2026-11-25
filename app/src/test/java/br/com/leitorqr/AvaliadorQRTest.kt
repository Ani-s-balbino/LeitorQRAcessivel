package br.com.leitorqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvaliadorQRTest {

    @Test
    fun qrMuitoPertoAvisaParaAfastarComTomDeErro() {
        val o = AvaliadorQR.avaliar(tamanho = 0.9f, dx = 0f, dy = 0f, inclinacao = 0f)
        assertEquals(0, o.nivel)
        assertTrue(o.muitoPerto)
        assertTrue(o.fala.contains("Afaste"))
    }

    @Test
    fun qrAEsquerdaPedeParaIrParaAEsquerda() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = -0.2f, dy = 0f, inclinacao = 0f)
        assertEquals(1, o.nivel)
        assertTrue(o.fala.endsWith("Mova o celular para a esquerda"))
    }

    @Test
    fun qrAcimaPedeParaIrParaCima() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = 0f, dy = -0.2f, inclinacao = 0f)
        assertEquals(1, o.nivel)
        assertTrue(o.fala.endsWith("Mova o celular para cima"))
    }

    @Test
    fun qrAbaixoPedeParaIrParaBaixo() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = 0f, dy = 0.2f, inclinacao = 0f)
        assertTrue(o.fala.endsWith("Mova o celular para baixo"))
    }

    @Test
    fun direcoesCombinadasSaemNaMesmaFrase() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = 0.2f, dy = -0.2f, inclinacao = 0f)
        assertTrue(o.fala.endsWith("Mova o celular para a direita e para cima"))
    }

    @Test
    fun foraDoCentroMuitoGrandeVaiParaNivelZero() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = -0.4f, dy = 0f, inclinacao = 0f)
        assertEquals(0, o.nivel)
    }

    @Test
    fun dentroDaToleranciaNaoPedeAjusteDeCentro() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = 0.14f, dy = -0.14f, inclinacao = 0f)
        assertEquals(3, o.nivel)
    }

    @Test
    fun qrMuitoLongeNivelZero() {
        val o = AvaliadorQR.avaliar(tamanho = 0.1f, dx = 0f, dy = 0f, inclinacao = 0f)
        assertEquals(0, o.nivel)
        assertTrue(o.fala.contains("muito longe"))
    }

    @Test
    fun umPoucoLongeENivelUm() {
        val o = AvaliadorQR.avaliar(tamanho = 0.25f, dx = 0f, dy = 0f, inclinacao = 0f)
        assertEquals(1, o.nivel)
        assertTrue(o.fala.contains("Aproxime mais"))
    }

    @Test
    fun umPoucoPertoENivelUm() {
        val o = AvaliadorQR.avaliar(tamanho = 0.75f, dx = 0f, dy = 0f, inclinacao = 0f)
        assertEquals(1, o.nivel)
        assertTrue(o.fala.contains("Afaste um pouco"))
    }

    @Test
    fun celularInclinadoENivelDois() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = 0f, dy = 0f, inclinacao = 0.3f)
        assertEquals(2, o.nivel)
        assertTrue(o.fala.contains("inclinado"))
    }

    @Test
    fun enquadramentoPerfeitoENivelTres() {
        val o = AvaliadorQR.avaliar(tamanho = 0.5f, dx = 0f, dy = 0f, inclinacao = 0.05f)
        assertEquals(3, o.nivel)
        assertFalse(o.muitoPerto)
        assertTrue(o.fala.contains("Segure parado"))
    }

    @Test
    fun inclinacaoDeFrenteEZero() {
        val s = AvaliadorQR.inclinacao(topo = 100f, direita = 100f, base = 100f, esquerda = 100f)
        assertEquals(0f, s, 0.0001f)
    }

    @Test
    fun inclinacaoDeTrapezioELimitadaPelaMaiorDiferenca() {
        // topo 100, base 50: diferença horizontal de 50%
        val s = AvaliadorQR.inclinacao(topo = 100f, direita = 100f, base = 50f, esquerda = 100f)
        assertEquals(0.5f, s, 0.0001f)
    }

    @Test
    fun inclinacaoComLadosZeroNaoDividePorZero() {
        val s = AvaliadorQR.inclinacao(topo = 0f, direita = 0f, base = 0f, esquerda = 0f)
        assertEquals(0f, s, 0.0001f)
    }
}

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

import kotlin.math.abs
import kotlin.math.max

/** Resultado de uma avaliação: o que falar e o quão bem enquadrado está o QR code. */
data class Orientacao(
    val fala: String,
    /** 0 = muito errado, 1 = ajuste médio, 2 = ajuste fino, 3 = perfeito. */
    val nivel: Int,
    /** true quando o QR está perto demais: o aviso sai com um tom de erro. */
    val muitoPerto: Boolean = false
)

/**
 * Regras de orientação da busca, sem depender do Android (para os testes unitários).
 * Os números são frações do quadro da câmera.
 */
object AvaliadorQR {
    const val TOLERANCIA_CENTRO = 0.15f      // quanto o QR pode estar fora do centro
    const val TAMANHO_MUITO_LONGE = 0.18f    // tamanho do QR / lado menor do quadro
    const val TAMANHO_UM_POUCO_LONGE = 0.30f
    const val TAMANHO_UM_POUCO_PERTO = 0.70f
    const val TAMANHO_MUITO_PERTO = 0.80f
    const val LIMITE_INCLINACAO = 0.18f      // diferença entre lados opostos do QR (perspectiva)
    const val FORA_DO_CENTRO_MUITO = 0.30f   // acima disso, o ajuste é grande

    /**
     * @param tamanho lado maior do QR dividido pelo lado menor do quadro
     * @param dx centro do QR em relação ao centro do quadro (-0.5 a 0.5); negativo = à esquerda
     * @param dy centro do QR em relação ao centro do quadro (-0.5 a 0.5); negativo = acima
     * @param inclinacao diferença entre lados opostos (0 = de frente)
     */
    fun avaliar(tamanho: Float, dx: Float, dy: Float, inclinacao: Float): Orientacao {
        // 1) Perto demais (QR cortado ou desfocado)
        if (tamanho > TAMANHO_MUITO_PERTO) {
            return Orientacao("QR code muito perto. Afaste o celular.", 0, muitoPerto = true)
        }

        // 2) Fora do centro: se o QR aparece à esquerda, o celular deve ir para a esquerda.
        val direcoes = mutableListOf<String>()
        if (dx < -TOLERANCIA_CENTRO) direcoes.add("a esquerda")
        if (dx > TOLERANCIA_CENTRO) direcoes.add("a direita")
        if (dy < -TOLERANCIA_CENTRO) direcoes.add("cima")
        if (dy > TOLERANCIA_CENTRO) direcoes.add("baixo")
        if (direcoes.isNotEmpty()) {
            val muitoFora = abs(dx) > FORA_DO_CENTRO_MUITO || abs(dy) > FORA_DO_CENTRO_MUITO
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
     * Inclinação pela perspectiva: se o celular está torto em relação à tela, lados opostos
     * do QR aparecem com tamanhos diferentes. 0 = de frente, quanto maior, mais torto.
     * Os lados são informados em ordem: topo, direita, base, esquerda.
     */
    fun inclinacao(topo: Float, direita: Float, base: Float, esquerda: Float): Float {
        val horizontal = abs(topo - base) / max(max(topo, base), 1f)
        val vertical = abs(esquerda - direita) / max(max(esquerda, direita), 1f)
        return max(horizontal, vertical)
    }
}

package com.elprofe.despiece.core

import java.util.TreeMap
import kotlin.math.ceil

class ResumenDiametro(val diametro: Int, val plan: PlanCorte) {
    val piezas: Int = plan.piezas
    val metros: Double = plan.largoUtil / 100.0
    val peso: Double = Acero.kgPorMetro(diametro) * metros
    val barrasComerciales: Int = plan.barras
    val pesoComprado: Double = Acero.kgPorMetro(diametro) * plan.largoComprado / 100.0
    val desperdicio: Double = plan.desperdicio
    /** Metros de barra que sobran al cortar. */
    val sobrante: Double = (plan.largoComprado - plan.largoUtil) / 100.0

    /** Barras comerciales a comprar añadiendo un margen (fracción: 0,05 = 5 %) sobre el plan de corte. */
    fun barrasConMargen(margen: Double): Int =
        if (margen <= 0.0) barrasComerciales else ceil(barrasComerciales * (1 + margen) - 1e-9).toInt()

    fun pesoConMargen(margen: Double): Double =
        Acero.kgPorMetro(diametro) * barrasConMargen(margen) * plan.largoComercial / 100.0
}

/** Despiece completo de una obra: planillas por elemento, totales por diámetro y plan de corte. */
class ResumenObra(
    val obra: Obra,
    val despieces: List<DespieceElemento>,
    val porDiametro: List<ResumenDiametro>,
) {
    val peso: Double = porDiametro.sumOf { it.peso }
    val pesoComprado: Double = porDiametro.sumOf { it.pesoComprado }
    val barrasComerciales: Int = porDiametro.sumOf { it.barrasComerciales }
    val desperdicio: Double = if (pesoComprado > 0) 1.0 - peso / pesoComprado else 0.0
    val sobrante: Double = porDiametro.sumOf { it.sobrante }

    /** Margen de desperdicio pedido en los ajustes, como fracción (0 = sin margen). */
    val margen: Double = maxOf(0.0, obra.ajustes.margenDesperdicio) / 100.0
    val barrasConMargen: Int = porDiametro.sumOf { it.barrasConMargen(margen) }
    val pesoConMargen: Double = porDiametro.sumOf { it.pesoConMargen(margen) }

    /** "Losas, vigas y pilares" según los elementos de la obra. */
    val tipoEstructura: String = run {
        val tipos = TipoElemento.entries.filter { t -> obra.elementos.any { it.tipo == t } }.map { it.plural }
        when (tipos.size) {
            0 -> "Sin elementos"
            1 -> tipos[0]
            else -> tipos.dropLast(1).joinToString(", ") + " y " + tipos.last()
        }.replaceFirstChar { it.uppercase() }
    }

    companion object {
        fun de(obra: Obra): ResumenObra {
            val calc = Calculadora(obra.ajustes)
            val despieces = obra.elementos.map { calc.calcular(it) }
            val piezas = TreeMap<Int, MutableMap<Int, Int>>()
            for (d in despieces) {
                for (b in d.barras) {
                    val n = b.cantidad * d.veces
                    if (n <= 0) continue
                    val porLargo = piezas.getOrPut(b.diametro) { HashMap() }
                    porLargo[b.longitud] = (porLargo[b.longitud] ?: 0) + n
                }
            }
            val largo = obra.ajustes.largoComercialCm
            val porDiametro = piezas.map { (diametro, porLargo) ->
                ResumenDiametro(diametro, Optimizador.planificar(diametro, porLargo, largo))
            }
            return ResumenObra(obra, despieces, porDiametro)
        }
    }
}

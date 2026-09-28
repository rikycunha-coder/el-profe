package com.elprofe.despiece.core

import java.util.TreeMap

class ResumenDiametro(val diametro: Int, val plan: PlanCorte) {
    val piezas: Int = plan.piezas
    val metros: Double = plan.largoUtil / 100.0
    val peso: Double = Acero.kgPorMetro(diametro) * metros
    val barrasComerciales: Int = plan.barras
    val pesoComprado: Double = Acero.kgPorMetro(diametro) * plan.largoComprado / 100.0
    val desperdicio: Double = plan.desperdicio
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

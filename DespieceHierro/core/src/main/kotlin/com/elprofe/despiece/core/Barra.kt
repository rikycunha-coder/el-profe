package com.elprofe.despiece.core

enum class Forma(val etiqueta: String) {
    RECTA("Recta"),
    L("L (una pata)"),
    U("U (dos patas)"),
    ESTRIBO("Estribo"),
    TRABA("Traba"),
    CIRCULO("Zuncho circular"),
}

/**
 * Una fila de la planilla de despiece: un tipo de barra con su forma y medidas.
 *
 * [tramos] depende de la forma (todo en cm):
 *  - RECTA:   [largo]
 *  - L:       [pata, largo recto]
 *  - U:       [pata inicial, largo recto, pata final]
 *  - ESTRIBO: [ancho a, alto b] + dos ganchos de [gancho]
 *  - TRABA:   [largo recto] + dos ganchos de [gancho]
 *  - CIRCULO: [diámetro] + dos ganchos de [gancho]
 */
data class Barra(
    val marca: Int,
    val descripcion: String,
    /** mm */
    val diametro: Int,
    val forma: Forma,
    val tramos: List<Int>,
    /** cm */
    val gancho: Int,
    /** Largo de corte de una barra, cm. */
    val longitud: Int,
    /** Barras por elemento. */
    val cantidad: Int,
) {
    val kgPorMetro: Double get() = Acero.kgPorMetro(diametro)

    /** kg de una barra. */
    val pesoUnitario: Double get() = kgPorMetro * longitud / 100.0

    /** kg de todas las barras de esta fila en un elemento. */
    val pesoTotal: Double get() = pesoUnitario * cantidad

    /** Metros de todas las barras de esta fila en un elemento. */
    val largoTotal: Double get() = longitud * cantidad / 100.0

    fun medidas(): String = when (forma) {
        Forma.RECTA -> "${tramos[0]}"
        Forma.L -> "${tramos[0]} + ${tramos[1]}"
        Forma.U -> "${tramos[0]} + ${tramos[1]} + ${tramos[2]}"
        Forma.ESTRIBO -> "${tramos[0]} × ${tramos[1]} + 2 ganchos de $gancho"
        Forma.TRABA -> "${tramos[0]} + 2 ganchos de $gancho"
        Forma.CIRCULO -> "Ø ${tramos[0]} + 2 ganchos de $gancho"
    }
}

data class TotalDiametro(val diametro: Int, val metros: Double, val kg: Double)

class DespieceElemento(
    val elemento: Elemento,
    val barras: List<Barra>,
    val notas: List<String>,
) {
    val veces: Int = maxOf(0, elemento.cantidad)

    val pesoPorElemento: Double = barras.sumOf { it.pesoTotal }

    val pesoTotal: Double = pesoPorElemento * veces

    val barrasPorElemento: Int = barras.sumOf { it.cantidad }

    /** Totales de todos los elementos iguales, agrupados por diámetro. */
    fun porDiametro(): List<TotalDiametro> =
        barras.groupBy { it.diametro }.toSortedMap().map { (d, filas) ->
            TotalDiametro(d, filas.sumOf { it.largoTotal } * veces, filas.sumOf { it.pesoTotal } * veces)
        }
}

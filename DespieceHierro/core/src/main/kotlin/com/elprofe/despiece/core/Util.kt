package com.elprofe.despiece.core

import java.math.BigDecimal
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToLong

private const val EPS = 1e-6

fun nuevoId(): String = UUID.randomUUID().toString()

/** Redondea al centímetro más cercano. */
fun redondear(x: Double): Int = x.roundToLong().toInt()

/** Redondea hacia arriba al centímetro, tolerando errores de coma flotante (2,9999999 → 3). */
fun redondearArriba(x: Double): Int = ceil(x - EPS).toInt()

/** División entera redondeando hacia arriba, para a >= 0 y b > 0. */
fun divArriba(a: Int, b: Int): Int = (a + b - 1) / b

/**
 * Número de barras necesarias para cubrir [largo] sin superar la separación [separacion]:
 * una barra en cada extremo y los huecos necesarios entre medias.
 */
fun porSeparacion(largo: Double, separacion: Double): Int {
    if (largo <= 0.0 || separacion <= 0.0) return 0
    return ceil(largo / separacion - EPS).toInt() + 1
}

/**
 * Estribos a lo largo de [largo] con zonas de confinamiento de [largoZona] en ambos
 * extremos (separación [separacionZona]) y separación [separacion] en el centro.
 */
fun estribosConZonas(largo: Double, separacion: Double, largoZona: Double, separacionZona: Double): Int {
    if (largo <= 0.0) return 0
    if (largoZona <= 0.0 || separacionZona <= 0.0) return porSeparacion(largo, separacion)
    if (2 * largoZona >= largo || separacion <= 0.0) return porSeparacion(largo, separacionZona)
    val huecos = 2 * ceil(largoZona / separacionZona - EPS).toInt() +
        ceil((largo - 2 * largoZona) / separacion - EPS).toInt()
    return huecos + 1
}

object Formato {
    private val es: Locale = Locale.forLanguageTag("es-ES")

    /** Número con separador de miles y coma decimal: 1.234,56 */
    fun num(x: Double, decimales: Int = 2): String = String.format(es, "%,.${decimales}f", x)

    /** Número sin separador de miles, para CSV: 1234,56 */
    fun plano(x: Double, decimales: Int = 2): String = String.format(es, "%.${decimales}f", x)

    fun kg(x: Double): String = num(x, 1)

    /** Centímetros enteros expresados en metros: 480 → "4,80" */
    fun metros(cm: Int): String = num(cm / 100.0, 2)

    fun porcentaje(fraccion: Double): String = num(fraccion * 100, 1) + " %"

    /** Valor para mostrar en un campo editable: sin ceros sobrantes y con coma decimal. */
    fun editable(x: Double): String =
        BigDecimal.valueOf(x).stripTrailingZeros().toPlainString().replace('.', ',')

    /** Lee un número escrito con coma o punto decimal. Devuelve null si no es válido o es negativo. */
    fun leer(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 && it.isFinite() }
}

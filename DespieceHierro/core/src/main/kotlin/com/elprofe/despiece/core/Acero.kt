package com.elprofe.despiece.core

object Acero {
    /** Diámetros comerciales de barras corrugadas, en mm. */
    val DIAMETROS: List<Int> = listOf(6, 8, 10, 12, 16, 18, 20, 22, 25, 28, 32, 36)

    private const val DENSIDAD_KG_M3 = 7850.0

    /** Masa lineal en kg/m: π/4 · Ø² · 7850 kg/m³ (Ø12 → 0,888 kg/m). */
    fun kgPorMetro(diametroMm: Int): Double =
        Math.PI / 4.0 * diametroMm * diametroMm * DENSIDAD_KG_M3 / 1_000_000.0
}

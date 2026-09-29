package com.elprofe.despiece.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculadoraTest {

    private val calc = Calculadora(Ajustes())

    private fun DespieceElemento.fila(descripcion: String): Barra =
        barras.single { it.descripcion == descripcion }

    @Test
    fun pesoPorMetroSegunTabla() {
        assertEquals(0.395, Acero.kgPorMetro(8), 0.001)
        assertEquals(0.617, Acero.kgPorMetro(10), 0.001)
        assertEquals(0.888, Acero.kgPorMetro(12), 0.001)
        assertEquals(1.578, Acero.kgPorMetro(16), 0.001)
        assertEquals(3.853, Acero.kgPorMetro(25), 0.001)
    }

    @Test
    fun cantidadPorSeparacion() {
        assertEquals(21, porSeparacion(395.0, 20.0))
        assertEquals(21, porSeparacion(400.0, 20.0))
        assertEquals(0, porSeparacion(0.0, 20.0))
        assertEquals(0, porSeparacion(100.0, 0.0))
    }

    @Test
    fun estribosConZonasDeConfinamiento() {
        assertEquals(21, estribosConZonas(300.0, 20.0, 50.0, 10.0))
        assertEquals(16, estribosConZonas(300.0, 20.0, 0.0, 10.0))
        // Las zonas cubren todo el largo: se usa la separación de zona.
        assertEquals(9, estribosConZonas(80.0, 20.0, 50.0, 10.0))
    }

    @Test
    fun losaMallaInferior() {
        val d = calc.calcular(Losa(largoX = 5.0, largoY = 4.0, recubrimiento = 2.5))
        val x = d.fila("Inferior dir. X")
        assertEquals(Forma.RECTA, x.forma)
        assertEquals(495, x.longitud)
        assertEquals(21, x.cantidad)
        val y = d.fila("Inferior dir. Y")
        assertEquals(395, y.longitud)
        assertEquals(26, y.cantidad)
        assertEquals(2, d.barras.size)
        assertEquals((21 * 4.95 + 26 * 3.95) * Acero.kgPorMetro(10), d.pesoTotal, 1e-9)
    }

    @Test
    fun losaConMallaSuperiorLlevaPatasAutomaticas() {
        val d = calc.calcular(Losa(largoX = 5.0, largoY = 4.0, conSuperior = true))
        val x = d.fila("Superior dir. X")
        // Ø8: pata 12 × 0,8 = 9,6 → 10 cm en cada extremo
        assertEquals(Forma.U, x.forma)
        assertEquals(listOf(10, 495, 10), x.tramos)
        assertEquals(515, x.longitud)
    }

    @Test
    fun vigaLargaSeCortaConTraslapo() {
        val d = calc.calcular(Viga(largo = 20.0, recubrimiento = 2.5))
        // 1995 recto + 2 patas de 20 (12 × 1,6 = 19,2) = 2035 cm; traslapo Ø16 = 80 cm
        val p1 = d.fila("Inferior · pieza 1 de 2")
        assertEquals(Forma.L, p1.forma)
        assertEquals(listOf(20, 1180), p1.tramos)
        assertEquals(1200, p1.longitud)
        assertEquals(3, p1.cantidad)
        val p2 = d.fila("Inferior · pieza 2 de 2")
        assertEquals(listOf(20, 895), p2.tramos)
        assertEquals(915, p2.longitud)
        assertEquals(2035 + 80, p1.longitud + p2.longitud)
        assertTrue(d.notas.any { "2 piezas" in it })
    }

    @Test
    fun piezasIntermediasIgualesSeAgrupan() {
        val extra = BarraExtra(descripcion = "Corrida", cantidad = 1, diametro = 12, largo = 3000.0)
        val d = calc.calcular(Losa(extras = listOf(extra)))
        val iguales = d.fila("Corrida · piezas 1–2 de 3")
        assertEquals(1200, iguales.longitud)
        assertEquals(2, iguales.cantidad)
        val ultima = d.fila("Corrida · pieza 3 de 3")
        assertEquals(720, ultima.longitud)
        assertEquals(3000 + 2 * 60, 2 * 1200 + 720)
    }

    @Test
    fun ultimaPiezaConservaElTraslapoEnRecto() {
        // 1160 + pata final de 50 = 1210 cm: la última pieza quedaría con 20 cm rectos.
        val extra = BarraExtra(descripcion = "Borde", cantidad = 1, diametro = 12, largo = 1160.0, pataFin = 50.0)
        val d = calc.calcular(Losa(extras = listOf(extra)))
        val p1 = d.fila("Borde · pieza 1 de 2")
        val p2 = d.fila("Borde · pieza 2 de 2")
        assertEquals(listOf(1160), p1.tramos)
        assertEquals(listOf(50, 60), p2.tramos)
        assertEquals(1210 + 60, p1.longitud + p2.longitud)
    }

    @Test
    fun pilarRectangular() {
        val d = calc.calcular(Pilar(b = 30.0, h = 30.0, altura = 3.0, recubrimiento = 2.5))
        val lon = d.fila("Longitudinales")
        assertEquals(Forma.L, lon.forma)
        assertEquals(listOf(20, 380), lon.tramos) // 300 + traslapo 80, pata 20
        assertEquals(4, lon.cantidad)
        val est = d.fila("Estribos")
        assertEquals(listOf(25, 25), est.tramos)
        assertEquals(8, est.gancho)
        assertEquals(116, est.longitud)
        assertEquals(25, est.cantidad)
    }

    @Test
    fun pilarConArmaduraDePiel() {
        val d = calc.calcular(
            Pilar(b = 40.0, h = 60.0, altura = 3.0, anclaje = 40.0, piel = GrupoBarras(cantidad = 6, diametro = 12)),
        )
        val esquinas = d.fila("Longitudinales")
        assertEquals(listOf(20, 40 + 300 + 80), esquinas.tramos)
        assertEquals(4, esquinas.cantidad)
        // La piel lleva su propio traslapo (Ø12 → 60 cm) y su pata automática (12 × 1,2 = 14,4 → 15 cm).
        val piel = d.fila("Piel (caras)")
        assertEquals(12, piel.diametro)
        assertEquals(Forma.L, piel.forma)
        assertEquals(listOf(15, 40 + 300 + 60), piel.tramos)
        assertEquals(6, piel.cantidad)
    }

    @Test
    fun pilarSinPielOCircularNoLlevaPiel() {
        assertTrue(calc.calcular(Pilar()).barras.none { it.descripcion.startsWith("Piel") })
        val circular = Pilar(circular = true, piel = GrupoBarras(cantidad = 6, diametro = 12))
        assertTrue(calc.calcular(circular).barras.none { it.descripcion.startsWith("Piel") })
    }

    @Test
    fun pilarCircularConZunchos() {
        val d = calc.calcular(Pilar(circular = true, diametroSeccion = 40.0, recubrimiento = 2.5))
        val z = d.fila("Zunchos")
        assertEquals(Forma.CIRCULO, z.forma)
        assertEquals(listOf(35), z.tramos)
        assertEquals(110 + 2 * 8, z.longitud) // π × 35 = 109,96 → 110 cm
    }

    @Test
    fun muroDobleMalla() {
        val d = calc.calcular(Muro(largo = 4.0, altura = 2.5, espesor = 20.0, recubrimiento = 2.5, trabasPorM2 = 4.0))
        val v = d.fila("Verticales")
        assertEquals(listOf(15, 310), v.tramos)
        assertEquals(42, v.cantidad)
        val h = d.fila("Horizontales")
        assertEquals(listOf(12, 395, 12), h.tramos)
        assertEquals(28, h.cantidad)
        val t = d.fila("Trabas")
        assertEquals(40, t.cantidad)
        assertEquals(15 + 2 * 8, t.longitud)
    }

    @Test
    fun elementosIgualesMultiplicanElPeso() {
        val una = calc.calcular(Viga(cantidad = 1))
        val tres = calc.calcular(Viga(cantidad = 3))
        assertEquals(una.pesoTotal * 3, tres.pesoTotal, 1e-9)
    }

    @Test
    fun separacionInvalidaDejaNota() {
        val d = calc.calcular(Losa(inferior = Malla(separacionX = 0.0)))
        assertTrue(d.barras.none { it.descripcion == "Inferior dir. X" })
        assertTrue(d.notas.isNotEmpty())
    }
}

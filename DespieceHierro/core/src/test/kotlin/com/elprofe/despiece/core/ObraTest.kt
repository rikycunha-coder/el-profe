package com.elprofe.despiece.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObraTest {

    @Test
    fun optimizadorAgrupaPiezasEnBarras() {
        val plan = Optimizador.planificar(12, mapOf(500 to 3), 1200)
        assertEquals(2, plan.barras)
        assertEquals(0.375, plan.desperdicio, 1e-9)
        assertEquals(listOf(500, 500), plan.patrones.first { it.piezas.size == 2 }.piezas)
    }

    @Test
    fun optimizadorSinDesperdicio() {
        val plan = Optimizador.planificar(12, mapOf(600 to 4, 400 to 3), 1200)
        assertEquals(3, plan.barras)
        assertEquals(0.0, plan.desperdicio, 1e-9)
        assertEquals("3 × 400", plan.patrones.single { it.piezas.first() == 400 }.descripcion())
    }

    @Test
    fun piezaMasLargaQueLaBarraComercial() {
        val plan = Optimizador.planificar(12, mapOf(1500 to 1), 1200)
        assertEquals(2, plan.barras)
        assertEquals(900, plan.patrones.single().sobrante)
    }

    @Test
    fun resumenCuadraConLosDespieces() {
        val obra = Almacen.obraDeEjemplo(0L)
        val r = ResumenObra.de(obra)
        assertEquals(r.despieces.sumOf { it.pesoTotal }, r.peso, 1e-6)
        assertTrue(r.pesoComprado >= r.peso)
        assertEquals(obra.pesoTotal(), r.peso, 1e-6)
        val piezas = r.despieces.sumOf { d -> d.barras.sumOf { it.cantidad * d.veces } }
        assertEquals(piezas, r.porDiametro.sumOf { it.piezas })
    }

    @Test
    fun guardarYLeerObras() {
        val obra = Almacen.obraDeEjemplo(123L).copy(
            elementos = Almacen.obraDeEjemplo(123L).elementos + Pilar(
                circular = true,
                extras = listOf(BarraExtra(forma = FormaExtra.ESTRIBO, descripcion = "Estribo; especial")),
            ),
        )
        val texto = Almacen.codificar(listOf(obra))
        assertTrue("\"clase\":\"losa\"" in texto)
        assertEquals(listOf(obra), Almacen.decodificar(texto))
    }

    @Test
    fun leerArchivoConCamposNuevosOFaltantes() {
        val texto = """{"obras":[{"id":"a","nombre":"X","campoFuturo":1,
            "elementos":[{"clase":"viga","id":"v","largo":6.5}]}]}"""
        val obras = Almacen.decodificar(texto)
        val viga = obras.single().elementos.single() as Viga
        assertEquals(6.5, viga.largo)
        assertEquals(Viga().inferior, viga.inferior)
    }

    @Test
    fun excelTieneHojasYFormulas() {
        val r = ResumenObra.de(Almacen.obraDeEjemplo(0L))
        val bytes = ExportadorExcel.generar(r, "Fecha: hoy")
        val partes = mutableMapOf<String, String>()
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { partes[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        assertTrue("[Content_Types].xml" in partes)
        assertTrue(partes.getValue("xl/workbook.xml").contains("name=\"Plan de corte\""))
        assertEquals(3, partes.keys.count { it.startsWith("xl/worksheets/sheet") })
        assertTrue(partes.getValue("xl/workbook.xml").indexOf("\"Resumen\"") < partes.getValue("xl/workbook.xml").indexOf("\"Planilla\""))
        val planilla = partes.getValue("xl/worksheets/sheet2.xml")
        assertTrue(planilla.contains("<f>L6*M6</f>"))
        val textos = partes.getValue("xl/sharedStrings.xml")
        assertTrue(textos.contains("Pilar P-1"))
        assertTrue(textos.contains("Resumen del proyecto"))
        assertTrue(textos.contains("Lista de compra consolidada"))
        assertTrue(textos.contains("Estribo cerrado: 3 dobleces a 90°"))
    }

    @Test
    fun margenDeDesperdicioVaAparteDelNeto() {
        val base = Almacen.obraDeEjemplo(0L)
        val sin = ResumenObra.de(base)
        val con = ResumenObra.de(base.copy(ajustes = base.ajustes.copy(margenDesperdicio = 5.0)))
        assertEquals(sin.peso, con.peso, 1e-9)
        assertEquals(sin.barrasComerciales, con.barrasComerciales)
        assertEquals(sin.barrasComerciales, sin.barrasConMargen)
        for (rd in con.porDiametro) {
            assertEquals(kotlin.math.ceil(rd.barrasComerciales * 1.05 - 1e-9).toInt(), rd.barrasConMargen(0.05))
        }
        assertTrue(con.pesoConMargen > con.pesoComprado)
        val excel = ExportadorExcel.generar(con, "hoy")
        val textos = java.util.zip.ZipInputStream(excel.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.first { it.name == "xl/sharedStrings.xml" }
            zip.readBytes().toString(Charsets.UTF_8)
        }
        assertTrue(textos.contains("Barras con margen 5,0 %"))
    }

    @Test
    fun tipoDeEstructuraSegunLosElementos() {
        assertEquals("Losas, vigas, muros y pilares", ResumenObra.de(Almacen.obraDeEjemplo(0L)).tipoEstructura)
        assertEquals("Pilares", ResumenObra.de(Obra(elementos = listOf(Pilar()))).tipoEstructura)
        assertEquals("Vigas y pilares", ResumenObra.de(Obra(elementos = listOf(Pilar(), Viga()))).tipoEstructura)
    }

    @Test
    fun columnasDeExcel() {
        assertEquals("A", LibroExcel.columna(0))
        assertEquals("Z", LibroExcel.columna(25))
        assertEquals("AA", LibroExcel.columna(26))
        assertEquals("AZ", LibroExcel.columna(51))
    }

    @Test
    fun croquisDeTodasLasFormas() {
        val b = Barra(1, "x", 12, Forma.RECTA, listOf(100), 0, 100, 1)
        val formas = mapOf(
            Forma.RECTA to listOf(100),
            Forma.L to listOf(15, 100),
            Forma.U to listOf(15, 100, 15),
            Forma.ESTRIBO to listOf(25, 45),
            Forma.TRABA to listOf(25),
            Forma.CIRCULO to listOf(35),
        )
        for ((forma, tramos) in formas) {
            val dibujo = Croquis.de(b.copy(forma = forma, tramos = tramos))
            assertTrue(dibujo.lineas.isNotEmpty() || dibujo.circulos.isNotEmpty())
            val puntos = dibujo.lineas.flatten() + dibujo.etiquetas.map { it.posicion }
            assertTrue(puntos.all { it.x in 0f..Croquis.ANCHO && it.y in 0f..Croquis.ALTO })
        }
    }

    @Test
    fun formatoNumerico() {
        assertEquals(2.5, Formato.leer("2,5"))
        assertEquals(2.5, Formato.leer(" 2.5 "))
        assertEquals(null, Formato.leer("-1"))
        assertEquals(null, Formato.leer("abc"))
        assertEquals("20", Formato.editable(20.0))
        assertEquals("2,5", Formato.editable(2.5))
        assertEquals("1.234,5", Formato.kg(1234.54))
    }
}

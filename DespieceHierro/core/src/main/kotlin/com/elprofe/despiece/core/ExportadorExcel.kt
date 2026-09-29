package com.elprofe.despiece.core

/**
 * Despiece de la obra en Excel (.xlsx) con tres hojas: planilla de barras, resumen y plan de corte.
 * Las cantidades totales, largos y pesos son fórmulas: si se cambia un dato en Excel, se recalcula.
 */
object ExportadorExcel {

    fun generar(r: ResumenObra, fecha: String): ByteArray {
        val libro = LibroExcel()
        planilla(libro, r, fecha)
        resumen(libro, r, fecha)
        cortes(libro, r)
        return libro.bytes()
    }

    private fun ajustesEnTexto(aj: Ajustes): String =
        "Barra comercial ${Formato.editable(aj.largoComercial)} m · traslapo ${Formato.editable(aj.empalmeDiametros)}Ø · " +
            "pata automática ${Formato.editable(aj.pataDiametros)}Ø · gancho de estribos ${Formato.editable(aj.ganchoDiametros)}Ø"

    private fun encabezados(h: LibroExcel.Hoja, vararg titulos: String) {
        val f = h.fila()
        titulos.forEach { f.texto(it, Estilo.ENCABEZADO) }
    }

    // ------------------------------------------------------------ Planilla

    private fun planilla(libro: LibroExcel, r: ResumenObra, fecha: String) {
        val h = libro.hoja("Planilla")
        h.anchos(22.0, 8.0, 8.0, 7.0, 32.0, 7.0, 14.0, 28.0, 10.0, 10.0, 9.0, 11.0, 8.0, 11.0)
        h.fila().texto("Despiece de hierro · ${r.obra.nombre}", Estilo.TITULO)
        h.combinar("A1:N1")
        h.fila().texto("$fecha · ${ajustesEnTexto(r.obra.ajustes)}", Estilo.NOTA)
        h.fila().texto(
            "Medidas exteriores en cm. Pesos con 7.850 kg/m³. Cant. total, largo total y peso son fórmulas.",
            Estilo.NOTA,
        )
        h.saltar()
        encabezados(
            h, "Elemento", "Tipo", "Iguales", "Marca", "Descripción", "Ø (mm)", "Forma", "Medidas (cm)",
            "Largo unitario (m)", "Cant. por elemento", "Cant. total", "Largo total (m)", "kg/m", "Peso (kg)",
        )
        val filaEncabezado = h.proximaFila - 1
        h.inmovilizarFilas(filaEncabezado)

        val primera = h.proximaFila
        for (d in r.despieces) {
            for (b in d.barras) {
                val n = h.proximaFila
                val total = b.cantidad * d.veces
                val largoTotal = b.longitud / 100.0 * total
                h.fila()
                    .texto(d.elemento.nombre)
                    .texto(d.elemento.tipo.etiqueta)
                    .entero(d.veces)
                    .entero(b.marca)
                    .texto(b.descripcion)
                    .entero(b.diametro)
                    .texto(b.forma.etiqueta)
                    .texto(b.medidas())
                    .numero(b.longitud / 100.0)
                    .entero(b.cantidad)
                    .formula("J$n*C$n", total.toDouble(), Estilo.ENTERO)
                    .formula("I$n*K$n", largoTotal)
                    .numero(b.kgPorMetro, Estilo.DECIMAL3)
                    .formula("L$n*M$n", largoTotal * b.kgPorMetro)
            }
        }
        val ultima = h.proximaFila - 1
        if (ultima >= primera) {
            h.autofiltro("A$filaEncabezado:N$ultima")
            val piezas = r.despieces.sumOf { d -> d.barras.sumOf { it.cantidad * d.veces } }
            val metros = r.despieces.sumOf { d -> d.barras.sumOf { it.largoTotal * d.veces } }
            val f = h.fila().texto("TOTAL", Estilo.TOTAL_TEXTO)
            repeat(9) { f.vacia(Estilo.TOTAL_TEXTO) }
            f.formula("SUM(K$primera:K$ultima)", piezas.toDouble(), Estilo.TOTAL_ENTERO)
                .formula("SUM(L$primera:L$ultima)", metros, Estilo.TOTAL_DECIMAL)
                .vacia(Estilo.TOTAL_TEXTO)
                .formula("SUM(N$primera:N$ultima)", r.peso, Estilo.TOTAL_DECIMAL)
        } else {
            h.fila().texto("La obra no tiene barras.", Estilo.NOTA)
        }
    }

    // ------------------------------------------------------------ Resumen

    private fun resumen(libro: LibroExcel, r: ResumenObra, fecha: String) {
        val h = libro.hoja("Resumen")
        h.anchos(28.0, 12.0, 14.0, 10.0, 13.0, 14.0, 16.0, 13.0)
        h.fila().texto("Resumen · ${r.obra.nombre}", Estilo.TITULO)
        h.combinar("A1:H1")
        h.fila().texto(fecha, Estilo.NOTA)
        h.saltar()

        val lc = r.obra.ajustes.largoComercial
        h.fila().texto("Acero por diámetro", Estilo.SUBTITULO)
        encabezados(
            h, "Ø (mm)", "Piezas", "Largo total (m)", "kg/m", "Peso (kg)",
            "Barras de ${Formato.editable(lc)} m", "Peso comprado (kg)", "Desperdicio",
        )
        val primera = h.proximaFila
        for (rd in r.porDiametro) {
            val n = h.proximaFila
            val kgm = Acero.kgPorMetro(rd.diametro)
            h.fila()
                .entero(rd.diametro)
                .entero(rd.piezas)
                .numero(rd.metros)
                .numero(kgm, Estilo.DECIMAL3)
                .formula("C$n*D$n", rd.peso)
                .entero(rd.barrasComerciales)
                .formula("F$n*${lcTexto(lc)}*D$n", rd.pesoComprado)
                .formula("IF(G$n>0,1-E$n/G$n,0)", rd.desperdicio, Estilo.PORCENTAJE)
        }
        val ultima = h.proximaFila - 1
        if (ultima >= primera) {
            val n = h.proximaFila
            h.fila()
                .texto("TOTAL", Estilo.TOTAL_TEXTO)
                .formula("SUM(B$primera:B$ultima)", r.porDiametro.sumOf { it.piezas }.toDouble(), Estilo.TOTAL_ENTERO)
                .formula("SUM(C$primera:C$ultima)", r.porDiametro.sumOf { it.metros }, Estilo.TOTAL_DECIMAL)
                .vacia(Estilo.TOTAL_TEXTO)
                .formula("SUM(E$primera:E$ultima)", r.peso, Estilo.TOTAL_DECIMAL)
                .formula("SUM(F$primera:F$ultima)", r.barrasComerciales.toDouble(), Estilo.TOTAL_ENTERO)
                .formula("SUM(G$primera:G$ultima)", r.pesoComprado, Estilo.TOTAL_DECIMAL)
                .formula("IF(G$n>0,1-E$n/G$n,0)", r.desperdicio, Estilo.TOTAL_PORCENTAJE)
        }
        h.saltar()

        h.fila().texto("Peso por elemento", Estilo.SUBTITULO)
        encabezados(h, "Elemento", "Tipo", "Iguales", "Peso c/u (kg)", "Peso total (kg)", "Barras")
        val primeraE = h.proximaFila
        for (d in r.despieces) {
            val n = h.proximaFila
            h.fila()
                .texto(d.elemento.nombre)
                .texto(d.elemento.tipo.etiqueta)
                .entero(d.veces)
                .numero(d.pesoPorElemento)
                .formula("C$n*D$n", d.pesoTotal)
                .entero(d.barrasPorElemento * d.veces)
        }
        val ultimaE = h.proximaFila - 1
        if (ultimaE >= primeraE) {
            h.fila()
                .texto("TOTAL", Estilo.TOTAL_TEXTO)
                .vacia(Estilo.TOTAL_TEXTO)
                .formula("SUM(C$primeraE:C$ultimaE)", r.despieces.sumOf { it.veces }.toDouble(), Estilo.TOTAL_ENTERO)
                .vacia(Estilo.TOTAL_TEXTO)
                .formula("SUM(E$primeraE:E$ultimaE)", r.peso, Estilo.TOTAL_DECIMAL)
                .formula(
                    "SUM(F$primeraE:F$ultimaE)",
                    r.despieces.sumOf { it.barrasPorElemento * it.veces }.toDouble(),
                    Estilo.TOTAL_ENTERO,
                )
        }
        h.saltar()

        val aj = r.obra.ajustes
        h.fila().texto("Criterios de cálculo", Estilo.SUBTITULO)
        fun criterio(etiqueta: String, valor: Double) {
            val n = h.proximaFila
            h.fila().texto(etiqueta, Estilo.ETIQUETA).vacia(Estilo.NORMAL).numero(valor)
            h.combinar("A$n:B$n")
        }
        criterio("Largo de la barra comercial (m)", aj.largoComercial)
        criterio("Traslapo (× Ø)", aj.empalmeDiametros)
        criterio("Pata automática (× Ø)", aj.pataDiametros)
        criterio("Gancho de estribos y trabas (× Ø)", aj.ganchoDiametros)
        h.fila().texto("Medidas exteriores, sin descontar el alargamiento por doblado.", Estilo.NOTA)

        val notas = r.despieces.flatMap { d -> d.notas.map { "${d.elemento.nombre}: $it" } }
        if (notas.isNotEmpty()) {
            h.saltar()
            h.fila().texto("Notas del cálculo", Estilo.SUBTITULO)
            notas.forEach { h.fila().texto(it, Estilo.NOTA) }
        }
    }

    private fun lcTexto(lc: Double): String = Formato.editable(lc).replace(',', '.')

    // ------------------------------------------------------------ Plan de corte

    private fun cortes(libro: LibroExcel, r: ResumenObra) {
        val h = libro.hoja("Plan de corte")
        h.anchos(9.0, 9.0, 60.0, 12.0, 14.0, 14.0)
        h.fila().texto("Plan de corte · ${r.obra.nombre}", Estilo.TITULO)
        h.combinar("A1:F1")
        h.fila().texto(
            "Cada fila es una forma de cortar la barra comercial de ${Formato.editable(r.obra.ajustes.largoComercial)} m. Piezas en cm.",
            Estilo.NOTA,
        )
        h.saltar()
        encabezados(h, "Ø (mm)", "Barras", "Piezas de cada barra (cm)", "Piezas por barra", "Sobrante por barra (cm)", "Sobrante total (m)")
        h.inmovilizarFilas(h.proximaFila - 1)
        val primera = h.proximaFila
        for (rd in r.porDiametro) {
            for (p in rd.plan.patrones) {
                val n = h.proximaFila
                val barras = p.veces * p.barrasPorPatron
                h.fila()
                    .entero(rd.diametro)
                    .entero(barras)
                    .texto(if (p.barrasPorPatron > 1) p.descripcion() + " (pieza a medida)" else p.descripcion())
                    .entero(p.piezas.size)
                    .entero(p.sobrante)
                    .let {
                        // Una pieza más larga que la barra comercial ocupa varias barras: su sobrante es del conjunto.
                        if (p.barrasPorPatron == 1) it.formula("B$n*E$n/100", p.veces * p.sobrante / 100.0)
                        else it.numero(p.veces * p.sobrante / 100.0)
                    }
            }
        }
        val ultima = h.proximaFila - 1
        if (ultima >= primera) {
            val sobrante = r.porDiametro.sumOf { rd -> rd.plan.patrones.sumOf { it.veces * it.sobrante / 100.0 } }
            h.fila()
                .texto("TOTAL", Estilo.TOTAL_TEXTO)
                .formula("SUM(B$primera:B$ultima)", r.barrasComerciales.toDouble(), Estilo.TOTAL_ENTERO)
                .vacia(Estilo.TOTAL_TEXTO)
                .vacia(Estilo.TOTAL_TEXTO)
                .vacia(Estilo.TOTAL_TEXTO)
                .formula("SUM(F$primera:F$ultima)", sobrante, Estilo.TOTAL_DECIMAL)
        }
    }
}

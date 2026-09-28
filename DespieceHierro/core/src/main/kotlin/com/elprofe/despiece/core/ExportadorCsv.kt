package com.elprofe.despiece.core

/** Planilla en CSV con punto y coma y coma decimal, lista para abrir en Excel en español. */
object ExportadorCsv {

    fun generar(r: ResumenObra): String = buildString {
        fun fila(vararg celdas: String) {
            append(celdas.joinToString(";") { escapar(it) })
            append("\r\n")
        }
        val aj = r.obra.ajustes
        fila("Obra", r.obra.nombre)
        fila("Largo de barra comercial (m)", Formato.plano(aj.largoComercial))
        fila("Traslapo (× Ø)", Formato.plano(aj.empalmeDiametros, 1))
        fila("Pata automática (× Ø)", Formato.plano(aj.pataDiametros, 1))
        fila("Gancho de estribos (× Ø)", Formato.plano(aj.ganchoDiametros, 1))
        fila()
        fila(
            "Elemento", "Tipo", "Elementos iguales", "Marca", "Descripción", "Ø (mm)", "Forma",
            "Medidas (cm)", "Largo unitario (m)", "Cant. por elemento", "Cant. total", "Largo total (m)", "Peso (kg)",
        )
        for (d in r.despieces) {
            for (b in d.barras) {
                fila(
                    d.elemento.nombre, d.elemento.tipo.etiqueta, d.veces.toString(), b.marca.toString(),
                    b.descripcion, b.diametro.toString(), b.forma.etiqueta, b.medidas(),
                    Formato.plano(b.longitud / 100.0), b.cantidad.toString(), (b.cantidad * d.veces).toString(),
                    Formato.plano(b.largoTotal * d.veces), Formato.plano(b.pesoTotal * d.veces),
                )
            }
        }
        fila()
        fila("Resumen por diámetro")
        fila("Ø (mm)", "Piezas", "Largo total (m)", "Peso (kg)", "Barras comerciales", "Peso comprado (kg)", "Desperdicio (%)")
        for (rd in r.porDiametro) {
            fila(
                rd.diametro.toString(), rd.piezas.toString(), Formato.plano(rd.metros), Formato.plano(rd.peso),
                rd.barrasComerciales.toString(), Formato.plano(rd.pesoComprado), Formato.plano(rd.desperdicio * 100, 1),
            )
        }
        fila(
            "TOTAL", "", "", Formato.plano(r.peso), r.barrasComerciales.toString(),
            Formato.plano(r.pesoComprado), Formato.plano(r.desperdicio * 100, 1),
        )
        fila()
        fila("Plan de corte")
        fila("Ø (mm)", "Barras", "Piezas por barra (cm)", "Sobrante por barra (cm)")
        for (rd in r.porDiametro) {
            for (pc in rd.plan.patrones) {
                fila(rd.diametro.toString(), (pc.veces * pc.barrasPorPatron).toString(), pc.descripcion(), pc.sobrante.toString())
            }
        }
    }

    private fun escapar(t: String): String =
        if (t.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
}

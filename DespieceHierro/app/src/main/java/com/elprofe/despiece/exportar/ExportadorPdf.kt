package com.elprofe.despiece.exportar

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.elprofe.despiece.core.Alineacion
import com.elprofe.despiece.core.Barra
import com.elprofe.despiece.core.Croquis
import com.elprofe.despiece.core.DespieceElemento
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.ResumenObra
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.min

/** Planilla de despiece en PDF (A4 vertical) con croquis de cada barra. */
object ExportadorPdf {

    fun crear(r: ResumenObra, archivo: File) {
        val doc = PdfDocument()
        try {
            val hoja = Hoja(doc, r.obra.nombre)
            hoja.portada(r)
            r.despieces.forEach { hoja.elemento(it) }
            hoja.resumen(r)
            hoja.cortes(r)
            hoja.terminar()
            FileOutputStream(archivo).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private class Columna(val titulo: String, val ancho: Float, val derecha: Boolean = false)

    private class Hoja(private val doc: PdfDocument, private val obra: String) {
        private val anchoPagina = 595
        private val altoPagina = 842
        private val margen = 32f
        private val limiteInferior = altoPagina - margen - 14f

        private var pagina: PdfDocument.Page? = null
        private var numero = 0
        private lateinit var canvas: Canvas
        private var y = 0f

        private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.BLACK }
        private val negrita = Paint(texto).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        private val titulo1 = Paint(negrita).apply { textSize = 16f }
        private val titulo2 = Paint(negrita).apply { textSize = 11f; color = Color.rgb(166, 66, 0) }
        private val gris = Paint(texto).apply { color = Color.rgb(90, 90, 90); textSize = 7f }
        private val cota = Paint(texto).apply { textSize = 5.5f }
        private val linea = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 0.5f; style = Paint.Style.STROKE }
        private val relleno = Paint().apply { color = Color.rgb(236, 236, 236); style = Paint.Style.FILL }
        private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(166, 66, 0)
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val columnasBarras = listOf(
            Columna("Nº", 18f),
            Columna("Forma (cm)", 86f),
            Columna("Descripción", 118f),
            Columna("Ø", 22f, true),
            Columna("Medidas (cm)", 104f),
            Columna("L (m)", 34f, true),
            Columna("Cant.", 34f, true),
            Columna("Total (m)", 44f, true),
            Columna("Peso (kg)", 46f, true),
        )

        private fun nuevaPagina() {
            terminarPagina()
            numero++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(anchoPagina, altoPagina, numero).create())
            pagina = p
            canvas = p.canvas
            canvas.drawText("Despiece de hierro · $obra", margen, margen, negrita)
            val pag = "Página $numero"
            canvas.drawText(pag, anchoPagina - margen - texto.measureText(pag), margen, texto)
            canvas.drawLine(margen, margen + 5f, anchoPagina - margen, margen + 5f, linea)
            canvas.drawText("Generado con la app Despiece de Hierro", margen, altoPagina - margen + 8f, gris)
            y = margen + 22f
        }

        private fun terminarPagina() {
            pagina?.let { doc.finishPage(it) }
            pagina = null
        }

        fun terminar() {
            if (pagina == null) nuevaPagina()
            terminarPagina()
        }

        /** Asegura [alto] puntos libres; si no caben, empieza otra página. Devuelve true si cambió de página. */
        private fun espacio(alto: Float): Boolean {
            if (pagina == null || y + alto > limiteInferior) {
                nuevaPagina()
                return true
            }
            return false
        }

        private fun recortar(t: String, ancho: Float, paint: Paint): String {
            if (paint.measureText(t) <= ancho) return t
            var s = t
            while (s.isNotEmpty() && paint.measureText("$s…") > ancho) s = s.dropLast(1)
            return "$s…"
        }

        /** Parte el texto en hasta [maxLineas] líneas que quepan en [ancho]. */
        private fun lineas(t: String, ancho: Float, paint: Paint, maxLineas: Int): List<String> {
            val resultado = mutableListOf<String>()
            var actual = ""
            for (palabra in t.split(" ")) {
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (paint.measureText(prueba) <= ancho || actual.isEmpty()) {
                    actual = prueba
                } else {
                    resultado += actual
                    actual = palabra
                }
            }
            if (actual.isNotEmpty()) resultado += actual
            if (resultado.size <= maxLineas) return resultado.map { recortar(it, ancho, paint) }
            val visibles = resultado.take(maxLineas).toMutableList()
            visibles[maxLineas - 1] = recortar(resultado.drop(maxLineas - 1).joinToString(" "), ancho, paint)
            return visibles
        }

        private fun celda(t: String, x: Float, col: Columna, base: Float, paint: Paint) {
            val s = recortar(t, col.ancho - 4f, paint)
            val xx = if (col.derecha) x + col.ancho - 2f - paint.measureText(s) else x + 2f
            canvas.drawText(s, xx, base, paint)
        }

        private fun encabezado(columnas: List<Columna>) {
            val alto = 14f
            canvas.drawRect(margen, y, margen + columnas.sumOf { it.ancho.toDouble() }.toFloat(), y + alto, relleno)
            var x = margen
            for (c in columnas) {
                celda(c.titulo, x, c, y + 10f, negrita)
                x += c.ancho
            }
            y += alto
        }

        private fun filaSimple(columnas: List<Columna>, valores: List<String>, paint: Paint = texto) {
            val alto = 13f
            if (espacio(alto)) encabezado(columnas)
            var x = margen
            columnas.forEachIndexed { i, c ->
                celda(valores[i], x, c, y + 9.5f, paint)
                x += c.ancho
            }
            y += alto
            canvas.drawLine(margen, y, margen + columnas.sumOf { it.ancho.toDouble() }.toFloat(), y, linea)
        }

        // ------------------------------------------------------------ Secciones

        fun portada(r: ResumenObra) {
            nuevaPagina()
            canvas.drawText(recortar(r.obra.nombre, anchoPagina - 2 * margen, titulo1), margen, y + 14f, titulo1)
            y += 26f
            val aj = r.obra.ajustes
            val datos = listOf(
                "Fecha: " + DateFormat.getDateInstance(DateFormat.LONG).format(Date()),
                "Elementos: ${r.despieces.size} · Acero colocado: ${Formato.kg(r.peso)} kg · A comprar: " +
                    "${r.barrasComerciales} barras (${Formato.kg(r.pesoComprado)} kg, desperdicio ${Formato.porcentaje(r.desperdicio)})",
                "Barra comercial ${Formato.editable(aj.largoComercial)} m · traslapo ${Formato.editable(aj.empalmeDiametros)}Ø · " +
                    "pata automática ${Formato.editable(aj.pataDiametros)}Ø · gancho de estribos ${Formato.editable(aj.ganchoDiametros)}Ø",
                "Medidas exteriores en cm, sin descontar alargamientos por doblado. Pesos con 7.850 kg/m³.",
            )
            for (d in datos) {
                canvas.drawText(recortar(d, anchoPagina - 2 * margen, texto), margen, y + 9f, texto)
                y += 12f
            }
            y += 8f
        }

        fun elemento(d: DespieceElemento) {
            val e = d.elemento
            // Título + encabezado + al menos una fila en la misma página.
            espacio(18f + 14f + 30f)
            val iguales = if (d.veces != 1) " · ${d.veces} iguales (${Formato.kg(d.pesoPorElemento)} kg c/u)" else ""
            val tituloElemento = "${e.nombre} — ${e.tipo.etiqueta}$iguales — ${Formato.kg(d.pesoTotal)} kg"
            canvas.drawText(recortar(tituloElemento, anchoPagina - 2 * margen, titulo2), margen, y + 12f, titulo2)
            y += 18f
            for (nota in d.notas) {
                espacio(11f)
                canvas.drawText(recortar("Nota: $nota", anchoPagina - 2 * margen, gris), margen, y + 8f, gris)
                y += 11f
            }
            if (d.barras.isEmpty()) {
                y += 8f
                return
            }
            encabezado(columnasBarras)
            for (b in d.barras) filaBarra(b, d.veces)
            y += 12f
        }

        private fun filaBarra(b: Barra, veces: Int) {
            val alto = 30f
            if (espacio(alto)) encabezado(columnasBarras)
            val cols = columnasBarras
            var x = margen
            val base = y + 12f
            val total = b.cantidad * veces
            val valores = listOf(
                b.marca.toString(),
                "",
                b.descripcion,
                b.diametro.toString(),
                b.medidas(),
                Formato.metros(b.longitud),
                if (veces > 1) "${b.cantidad}×$veces" else "$total",
                Formato.num(b.largoTotal * veces),
                Formato.num(b.pesoTotal * veces),
            )
            cols.forEachIndexed { i, c ->
                when (i) {
                    1 -> croquis(b, x + 2f, y + 3f, c.ancho - 4f, alto - 6f)
                    2, 4 -> lineas(valores[i], c.ancho - 4f, texto, 2).forEachIndexed { n, l ->
                        canvas.drawText(l, x + 2f, base + n * 9.5f, texto)
                    }
                    else -> celda(valores[i], x, c, base, texto)
                }
                x += c.ancho
            }
            if (veces > 1) {
                val c = cols[6]
                val xCant = margen + cols.take(6).sumOf { it.ancho.toDouble() }.toFloat()
                celda("= $total", xCant, c, base + 9.5f, gris)
            }
            y += alto
            canvas.drawLine(margen, y, margen + cols.sumOf { it.ancho.toDouble() }.toFloat(), y, linea)
        }

        private fun croquis(b: Barra, x: Float, y0: Float, ancho: Float, alto: Float) {
            val dibujo = Croquis.de(b)
            val escala = min(ancho / Croquis.ANCHO, alto / Croquis.ALTO)
            val dx = x + (ancho - Croquis.ANCHO * escala) / 2f
            val dy = y0 + (alto - Croquis.ALTO * escala) / 2f
            for (l in dibujo.lineas) {
                val path = Path()
                l.forEachIndexed { i, p ->
                    val px = dx + p.x * escala
                    val py = dy + p.y * escala
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, trazo)
            }
            for (c in dibujo.circulos) {
                canvas.drawCircle(dx + c.centro.x * escala, dy + c.centro.y * escala, c.radio * escala, trazo)
            }
            for (e in dibujo.etiquetas) {
                val w = cota.measureText(e.texto)
                val px = dx + e.posicion.x * escala
                val tx = when (e.alineacion) {
                    Alineacion.IZQUIERDA -> px
                    Alineacion.CENTRO -> px - w / 2f
                    Alineacion.DERECHA -> px - w
                }
                canvas.drawText(e.texto, tx, dy + e.posicion.y * escala + cota.textSize / 2.8f, cota)
            }
        }

        fun resumen(r: ResumenObra) {
            espacio(22f + 14f + 13f * (r.porDiametro.size + 1))
            canvas.drawText("Resumen por diámetro", margen, y + 12f, titulo2)
            y += 20f
            val lc = Formato.editable(r.obra.ajustes.largoComercial)
            val cols = listOf(
                Columna("Ø (mm)", 50f),
                Columna("Piezas", 60f, true),
                Columna("Largo (m)", 75f, true),
                Columna("Peso (kg)", 75f, true),
                Columna("Barras de $lc m", 85f, true),
                Columna("Comprado (kg)", 85f, true),
                Columna("Desperdicio", 70f, true),
            )
            encabezado(cols)
            for (rd in r.porDiametro) {
                filaSimple(
                    cols,
                    listOf(
                        "Ø${rd.diametro}", rd.piezas.toString(), Formato.num(rd.metros), Formato.num(rd.peso),
                        rd.barrasComerciales.toString(), Formato.num(rd.pesoComprado), Formato.porcentaje(rd.desperdicio),
                    ),
                )
            }
            filaSimple(
                cols,
                listOf(
                    "TOTAL", r.porDiametro.sumOf { it.piezas }.toString(), Formato.num(r.porDiametro.sumOf { it.metros }),
                    Formato.num(r.peso), r.barrasComerciales.toString(), Formato.num(r.pesoComprado),
                    Formato.porcentaje(r.desperdicio),
                ),
                negrita,
            )
            y += 14f
        }

        fun cortes(r: ResumenObra) {
            espacio(40f)
            canvas.drawText("Plan de corte (piezas en cm)", margen, y + 12f, titulo2)
            y += 20f
            val cols = listOf(Columna("Barras", 45f, true), Columna("Piezas de cada barra", 380f), Columna("Sobra (cm)", 75f, true))
            for (rd in r.porDiametro) {
                espacio(16f + 14f + 13f)
                canvas.drawText(
                    "Ø${rd.diametro} — ${rd.barrasComerciales} barras, desperdicio ${Formato.porcentaje(rd.desperdicio)}",
                    margen, y + 10f, negrita,
                )
                y += 16f
                encabezado(cols)
                for (p in rd.plan.patrones) {
                    val sobra = if (p.barrasPorPatron > 1) "a medida" else p.sobrante.toString()
                    filaSimple(cols, listOf((p.veces * p.barrasPorPatron).toString(), p.descripcion(), sobra))
                }
                y += 10f
            }
        }
    }
}

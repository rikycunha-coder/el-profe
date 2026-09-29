package com.elprofe.despiece.core

import java.io.OutputStream
import java.math.BigDecimal
import java.math.MathContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Estilos de celda disponibles; el orden coincide con cellXfs de styles.xml. */
enum class Estilo {
    NORMAL, TITULO, ENCABEZADO, TEXTO, ENTERO, DECIMAL, DECIMAL3, PORCENTAJE,
    TOTAL_TEXTO, TOTAL_ENTERO, TOTAL_DECIMAL, TOTAL_PORCENTAJE, NOTA, ETIQUETA, SUBTITULO,
}

/**
 * Escritor mínimo de archivos Excel (.xlsx, Office Open XML) sin dependencias:
 * varias hojas, textos, números, fórmulas con su valor calculado, anchos de columna,
 * filas inmovilizadas y autofiltro.
 */
class LibroExcel {

    internal sealed class Celda(val estilo: Estilo) {
        class Texto(val valor: String, estilo: Estilo) : Celda(estilo)
        class Numero(val valor: Double, estilo: Estilo) : Celda(estilo)
        class Formula(val formula: String, val valor: Double, estilo: Estilo) : Celda(estilo)
        class Vacia(estilo: Estilo) : Celda(estilo)
    }

    class Fila internal constructor(val numero: Int) {
        internal val celdas = mutableListOf<Celda>()

        /** Referencia de la próxima celda que se añadirá (p. ej. "C5"). */
        val siguiente: String get() = columna(celdas.size) + numero

        fun texto(valor: String, estilo: Estilo = Estilo.TEXTO) = apply { celdas += Celda.Texto(valor, estilo) }
        fun numero(valor: Double, estilo: Estilo = Estilo.DECIMAL) = apply { celdas += Celda.Numero(valor, estilo) }
        fun entero(valor: Int, estilo: Estilo = Estilo.ENTERO) = apply { celdas += Celda.Numero(valor.toDouble(), estilo) }
        fun formula(formula: String, valor: Double, estilo: Estilo = Estilo.DECIMAL) =
            apply { celdas += Celda.Formula(formula, valor, estilo) }
        fun vacia(estilo: Estilo = Estilo.TEXTO) = apply { celdas += Celda.Vacia(estilo) }
    }

    class Hoja internal constructor(val nombre: String) {
        internal val filas = mutableListOf<Fila>()
        internal val combinadas = mutableListOf<String>()
        internal var anchos: List<Double> = emptyList()
        internal var filasFijas = 0
        internal var filtro: String? = null

        /** Número (1..n) que tendrá la próxima fila. */
        val proximaFila: Int get() = filas.size + 1

        fun fila(): Fila = Fila(proximaFila).also { filas += it }

        fun saltar(n: Int = 1) = repeat(n) { fila() }

        fun anchos(vararg caracteres: Double) {
            anchos = caracteres.toList()
        }

        fun inmovilizarFilas(n: Int) {
            filasFijas = n
        }

        fun autofiltro(rango: String) {
            filtro = rango
        }

        fun combinar(rango: String) {
            combinadas += rango
        }
    }

    private val hojas = mutableListOf<Hoja>()
    private val cadenas = LinkedHashMap<String, Int>()

    fun hoja(nombre: String): Hoja = Hoja(nombre.take(31)).also { hojas += it }

    fun escribir(salida: OutputStream) {
        val xmlHojas = hojas.map { xmlHoja(it) } // antes que sharedStrings, que se llena aquí
        ZipOutputStream(salida).use { zip ->
            fun entrada(ruta: String, contenido: String) {
                zip.putNextEntry(ZipEntry(ruta))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entrada("[Content_Types].xml", tiposDeContenido())
            entrada("_rels/.rels", RELS_RAIZ)
            entrada("docProps/app.xml", APP)
            entrada("xl/workbook.xml", libro())
            entrada("xl/_rels/workbook.xml.rels", relacionesLibro())
            entrada("xl/styles.xml", ESTILOS)
            entrada("xl/sharedStrings.xml", textosCompartidos())
            xmlHojas.forEachIndexed { i, xml -> entrada("xl/worksheets/sheet${i + 1}.xml", xml) }
        }
    }

    fun bytes(): ByteArray = java.io.ByteArrayOutputStream().also { escribir(it) }.toByteArray()

    // ------------------------------------------------------------ Partes del paquete

    private fun tiposDeContenido() = buildString {
        append(CABECERA)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        hojas.indices.forEach {
            append("<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        append("<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>")
        append("<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>")
        append("</Types>")
    }

    private fun libro() = buildString {
        append(CABECERA)
        append("<workbook xmlns=\"$NS\" xmlns:r=\"$NS_R\"><bookViews><workbookView/></bookViews><sheets>")
        hojas.forEachIndexed { i, h ->
            append("<sheet name=\"${xml(h.nombre)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        append("</sheets>")
        val filtros = hojas.withIndex().filter { it.value.filtro != null }
        if (filtros.isNotEmpty()) {
            append("<definedNames>")
            for ((i, h) in filtros) {
                val rango = h.filtro!!.split(":").joinToString(":") { absoluta(it) }
                append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"$i\" hidden=\"1\">")
                append(xml("'${h.nombre.replace("'", "''")}'!$rango"))
                append("</definedName>")
            }
            append("</definedNames>")
        }
        append("<calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/></workbook>")
    }

    private fun relacionesLibro() = buildString {
        append(CABECERA)
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        hojas.indices.forEach {
            append("<Relationship Id=\"rId${it + 1}\" Type=\"$NS_R/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>")
        }
        val n = hojas.size
        append("<Relationship Id=\"rId${n + 1}\" Type=\"$NS_R/styles\" Target=\"styles.xml\"/>")
        append("<Relationship Id=\"rId${n + 2}\" Type=\"$NS_R/sharedStrings\" Target=\"sharedStrings.xml\"/>")
        append("</Relationships>")
    }

    private fun textosCompartidos() = buildString {
        append(CABECERA)
        append("<sst xmlns=\"$NS\" count=\"${cadenas.size}\" uniqueCount=\"${cadenas.size}\">")
        for (s in cadenas.keys) append("<si><t xml:space=\"preserve\">${xml(s)}</t></si>")
        append("</sst>")
    }

    private fun indiceTexto(s: String): Int = cadenas.getOrPut(s) { cadenas.size }

    private fun xmlHoja(h: Hoja) = buildString {
        append(CABECERA)
        append("<worksheet xmlns=\"$NS\" xmlns:r=\"$NS_R\">")
        append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
        append("<sheetViews><sheetView workbookViewId=\"0\">")
        if (h.filasFijas > 0) {
            val celda = "A${h.filasFijas + 1}"
            append("<pane ySplit=\"${h.filasFijas}\" topLeftCell=\"$celda\" activePane=\"bottomLeft\" state=\"frozen\"/>")
            append("<selection pane=\"bottomLeft\" activeCell=\"$celda\" sqref=\"$celda\"/>")
        }
        append("</sheetView></sheetViews>")
        append("<sheetFormatPr defaultRowHeight=\"15\"/>")
        if (h.anchos.isNotEmpty()) {
            append("<cols>")
            h.anchos.forEachIndexed { i, w ->
                append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"${num(w)}\" customWidth=\"1\"/>")
            }
            append("</cols>")
        }
        append("<sheetData>")
        for (f in h.filas) {
            if (f.celdas.isEmpty()) continue
            append("<row r=\"${f.numero}\">")
            f.celdas.forEachIndexed { i, c ->
                val ref = columna(i) + f.numero
                val s = c.estilo.ordinal
                when (c) {
                    is Celda.Texto -> append("<c r=\"$ref\" s=\"$s\" t=\"s\"><v>${indiceTexto(c.valor)}</v></c>")
                    is Celda.Numero -> append("<c r=\"$ref\" s=\"$s\"><v>${num(c.valor)}</v></c>")
                    is Celda.Formula -> append("<c r=\"$ref\" s=\"$s\"><f>${xml(c.formula)}</f><v>${num(c.valor)}</v></c>")
                    is Celda.Vacia -> append("<c r=\"$ref\" s=\"$s\"/>")
                }
            }
            append("</row>")
        }
        append("</sheetData>")
        h.filtro?.let { append("<autoFilter ref=\"$it\"/>") }
        if (h.combinadas.isNotEmpty()) {
            append("<mergeCells count=\"${h.combinadas.size}\">")
            h.combinadas.forEach { append("<mergeCell ref=\"$it\"/>") }
            append("</mergeCells>")
        }
        append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>")
        append("<pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
        append("</worksheet>")
    }

    companion object {
        /** Índice de columna (0 = A) a letras: 0 → A, 25 → Z, 26 → AA. */
        fun columna(indice: Int): String {
            var n = indice + 1
            val sb = StringBuilder()
            while (n > 0) {
                val r = (n - 1) % 26
                sb.insert(0, ('A' + r))
                n = (n - 1) / 26
            }
            return sb.toString()
        }

        private fun absoluta(ref: String): String {
            val letras = ref.takeWhile { it.isLetter() }
            return "$" + letras + "$" + ref.drop(letras.length)
        }

        /** Número en el formato de XML de Excel (punto decimal, 15 cifras significativas). */
        private fun num(x: Double): String =
            if (x.isFinite()) BigDecimal(x).round(MathContext(15)).stripTrailingZeros().toPlainString() else "0"

        private fun xml(s: String): String = buildString {
            for (ch in s) {
                when {
                    ch == '&' -> append("&amp;")
                    ch == '<' -> append("&lt;")
                    ch == '>' -> append("&gt;")
                    ch == '"' -> append("&quot;")
                    ch == '\t' || ch == '\n' || ch == '\r' -> append(ch)
                    ch < ' ' -> {} // caracteres de control no válidos en XML
                    else -> append(ch)
                }
            }
        }

        private const val CABECERA = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

        private const val RELS_RAIZ = CABECERA +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"$NS_R/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"$NS_R/extended-properties\" Target=\"docProps/app.xml\"/>" +
            "</Relationships>"

        private const val APP = CABECERA +
            "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\">" +
            "<Application>Despiece de Hierro</Application></Properties>"

        // Fuentes: 0 normal, 1 negrita, 2 título, 3 nota gris cursiva, 4 subtítulo.
        // Rellenos: 0 ninguno, 1 gray125 (obligatorio), 2 encabezado, 3 totales.
        // Bordes: 0 ninguno, 1 fino.
        // Formatos: 164 = #,##0.000 · 165 = 0.0% · 3 = #,##0 · 4 = #,##0.00
        private val ESTILOS = CABECERA +
            "<styleSheet xmlns=\"$NS\">" +
            "<numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.000\"/><numFmt numFmtId=\"165\" formatCode=\"0.0%\"/></numFmts>" +
            "<fonts count=\"5\">" +
            "<font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" +
            "<font><b/><sz val=\"16\"/><color rgb=\"FFA64200\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" +
            "<font><i/><sz val=\"10\"/><color rgb=\"FF595959\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" +
            "<font><b/><sz val=\"13\"/><color rgb=\"FFA64200\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" +
            "</fonts>" +
            "<fills count=\"4\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFDBCB\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEDEDED\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "</fills>" +
            "<borders count=\"2\">" +
            "<border><left/><right/><top/><bottom/><diagonal/></border>" +
            "<border><left style=\"thin\"><color rgb=\"FFBFBFBF\"/></left><right style=\"thin\"><color rgb=\"FFBFBFBF\"/></right>" +
            "<top style=\"thin\"><color rgb=\"FFBFBFBF\"/></top><bottom style=\"thin\"><color rgb=\"FFBFBFBF\"/></bottom><diagonal/></border>" +
            "</borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"${Estilo.entries.size}\">" +
            xf(0, 0, 0, 0) + // NORMAL
            xf(0, 2, 0, 0) + // TITULO
            xf(0, 1, 2, 1, "<alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/>") + // ENCABEZADO
            xf(0, 0, 0, 1, "<alignment vertical=\"center\" wrapText=\"1\"/>") + // TEXTO
            xf(3, 0, 0, 1, CENTRADO) + // ENTERO
            xf(4, 0, 0, 1, CENTRADO) + // DECIMAL
            xf(164, 0, 0, 1, CENTRADO) + // DECIMAL3
            xf(165, 0, 0, 1, CENTRADO) + // PORCENTAJE
            xf(0, 1, 3, 1, CENTRADO) + // TOTAL_TEXTO
            xf(3, 1, 3, 1, CENTRADO) + // TOTAL_ENTERO
            xf(4, 1, 3, 1, CENTRADO) + // TOTAL_DECIMAL
            xf(165, 1, 3, 1, CENTRADO) + // TOTAL_PORCENTAJE
            xf(0, 3, 0, 0) + // NOTA
            xf(0, 1, 0, 0) + // ETIQUETA
            xf(0, 4, 0, 0) + // SUBTITULO
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

        private const val CENTRADO = "<alignment vertical=\"center\"/>"

        private fun xf(formato: Int, fuente: Int, relleno: Int, borde: Int, alineacion: String = ""): String {
            val atributos = "numFmtId=\"$formato\" fontId=\"$fuente\" fillId=\"$relleno\" borderId=\"$borde\" xfId=\"0\"" +
                (if (formato != 0) " applyNumberFormat=\"1\"" else "") +
                (if (fuente != 0) " applyFont=\"1\"" else "") +
                (if (relleno != 0) " applyFill=\"1\"" else "") +
                (if (borde != 0) " applyBorder=\"1\"" else "") +
                (if (alineacion.isNotEmpty()) " applyAlignment=\"1\"" else "")
            return if (alineacion.isEmpty()) "<xf $atributos/>" else "<xf $atributos>$alineacion</xf>"
        }
    }
}

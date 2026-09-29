package com.elprofe.despiece.exportar

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.elprofe.despiece.core.ExportadorExcel
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.core.ResumenObra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date

enum class FormatoArchivo(val etiqueta: String, val extension: String, val mime: String) {
    EXCEL("Excel", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("PDF", "pdf", "application/pdf"),
}

/** Genera el Excel o el PDF de una obra para compartirlo o guardarlo en el teléfono. */
object Exportar {

    fun nombreArchivo(obra: Obra, formato: FormatoArchivo): String {
        val nombre = obra.nombre
            .replace(Regex("[^A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ _-]"), "")
            .trim()
            .replace(' ', '_')
            .ifEmpty { "obra" }
        return "Despiece_$nombre.${formato.extension}"
    }

    /** Crea el archivo en la carpeta temporal de la app y abre el menú de compartir de Android. */
    suspend fun compartir(context: Context, obra: Obra, formato: FormatoArchivo) {
        val archivo = withContext(Dispatchers.IO) {
            val carpeta = File(context.cacheDir, "exportados").apply { mkdirs() }
            File(carpeta, nombreArchivo(obra, formato)).also { f -> f.outputStream().use { escribir(obra, formato, it) } }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = formato.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Despiece de hierro · ${obra.nombre}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ${formato.etiqueta}"))
    }

    /** Escribe el archivo en el lugar que eligió el usuario (Descargas, Drive…). */
    suspend fun guardar(context: Context, obra: Obra, formato: FormatoArchivo, destino: Uri) = withContext(Dispatchers.IO) {
        val salida = context.contentResolver.openOutputStream(destino)
            ?: throw IOException("no se pudo abrir el archivo de destino")
        salida.use { escribir(obra, formato, it) }
    }

    private fun escribir(obra: Obra, formato: FormatoArchivo, salida: OutputStream) {
        val resumen = ResumenObra.de(obra)
        when (formato) {
            FormatoArchivo.EXCEL -> {
                val fecha = "Fecha: " + DateFormat.getDateInstance(DateFormat.LONG).format(Date())
                salida.write(ExportadorExcel.generar(resumen, fecha))
            }
            FormatoArchivo.PDF -> ExportadorPdf.crear(resumen, salida)
        }
    }
}

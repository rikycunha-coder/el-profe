package com.elprofe.despiece.exportar

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.elprofe.despiece.core.ExportadorCsv
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.core.ResumenObra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Genera el PDF o el CSV y abre el menú de Android para enviarlo o guardarlo. */
object Compartir {

    suspend fun pdf(context: Context, obra: Obra) {
        val archivo = withContext(Dispatchers.Default) {
            val destino = archivo(context, obra, "pdf")
            ExportadorPdf.crear(ResumenObra.de(obra), destino)
            destino
        }
        enviar(context, archivo, "application/pdf")
    }

    suspend fun csv(context: Context, obra: Obra) {
        val archivo = withContext(Dispatchers.Default) {
            val destino = archivo(context, obra, "csv")
            // La marca BOM hace que Excel reconozca los acentos (UTF-8).
            destino.writeText("﻿" + ExportadorCsv.generar(ResumenObra.de(obra)))
            destino
        }
        enviar(context, archivo, "text/csv")
    }

    private fun archivo(context: Context, obra: Obra, extension: String): File {
        val carpeta = File(context.cacheDir, "exportados").apply { mkdirs() }
        val nombre = obra.nombre.replace(Regex("[^A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ _-]"), "").trim().replace(' ', '_').ifEmpty { "obra" }
        return File(carpeta, "Despiece_$nombre.$extension")
    }

    private fun enviar(context: Context, archivo: File, tipo: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, archivo.nameWithoutExtension.replace('_', ' '))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir despiece"))
    }
}

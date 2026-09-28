package com.elprofe.despiece

import android.content.Context
import com.elprofe.despiece.core.Almacen
import com.elprofe.despiece.core.Obra
import java.io.File

/** Guarda las obras en un archivo JSON dentro de la memoria privada de la app. */
class Repositorio(context: Context) {
    private val carpeta = context.filesDir
    private val archivo = File(carpeta, "obras.json")

    /** Devuelve null si es el primer arranque (no hay archivo). */
    fun cargar(): List<Obra>? {
        if (!archivo.exists()) return null
        return try {
            Almacen.decodificar(archivo.readText())
        } catch (e: Exception) {
            // No se pierde nada: se aparta el archivo dañado y se empieza de cero.
            archivo.copyTo(File(carpeta, "obras-danado-${System.currentTimeMillis()}.json"), overwrite = true)
            emptyList()
        }
    }

    @Synchronized
    fun guardar(obras: List<Obra>) {
        val temporal = File(carpeta, "obras.json.tmp")
        temporal.writeText(Almacen.codificar(obras))
        if (!temporal.renameTo(archivo)) {
            archivo.delete()
            temporal.renameTo(archivo)
        }
    }
}

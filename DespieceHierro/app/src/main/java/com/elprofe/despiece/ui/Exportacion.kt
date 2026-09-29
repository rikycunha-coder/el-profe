package com.elprofe.despiece.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.exportar.Exportar
import com.elprofe.despiece.exportar.FormatoArchivo
import kotlinx.coroutines.launch

/**
 * Diálogo para exportar la obra a Excel o PDF, compartiéndola o guardándola en el teléfono.
 * Debe estar siempre en la composición de la pantalla (no solo cuando [visible]), porque
 * registra los selectores de archivo de Android que devuelven dónde guardar.
 */
@Composable
fun ExportacionObra(obra: Obra, visible: Boolean, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val obraActual by rememberUpdatedState(obra)
    var ocupado by remember { mutableStateOf(false) }

    fun ejecutar(mensaje: String?, accion: suspend () -> Unit) {
        ocupado = true
        alcance.launch {
            try {
                accion()
                if (mensaje != null) Toast.makeText(contexto, mensaje, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(contexto, "No se pudo exportar: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            } finally {
                ocupado = false
            }
        }
    }

    val guardarExcel = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FormatoArchivo.EXCEL.mime)) { uri ->
        if (uri != null) ejecutar("Excel guardado") { Exportar.guardar(contexto, obraActual, FormatoArchivo.EXCEL, uri) }
    }
    val guardarPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FormatoArchivo.PDF.mime)) { uri ->
        if (uri != null) ejecutar("PDF guardado") { Exportar.guardar(contexto, obraActual, FormatoArchivo.PDF, uri) }
    }

    fun guardar(formato: FormatoArchivo) {
        onCerrar()
        val nombre = Exportar.nombreArchivo(obraActual, formato)
        try {
            when (formato) {
                FormatoArchivo.EXCEL -> guardarExcel.launch(nombre)
                FormatoArchivo.PDF -> guardarPdf.launch(nombre)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(contexto, "Este teléfono no tiene gestor de archivos: usa «Compartir».", Toast.LENGTH_LONG).show()
        }
    }

    fun compartir(formato: FormatoArchivo) {
        onCerrar()
        ejecutar(null) { Exportar.compartir(contexto, obraActual, formato) }
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Exportar obra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(obra.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OpcionFormato(
                        titulo = "Excel (.xlsx)",
                        descripcion = "Planilla de barras, resumen por diámetro y plan de corte en hojas separadas, con fórmulas.",
                        onCompartir = { compartir(FormatoArchivo.EXCEL) },
                        onGuardar = { guardar(FormatoArchivo.EXCEL) },
                    )
                    OpcionFormato(
                        titulo = "PDF",
                        descripcion = "Planilla para imprimir con el dibujo de cada barra, resumen y plan de corte.",
                        onCompartir = { compartir(FormatoArchivo.PDF) },
                        onGuardar = { guardar(FormatoArchivo.PDF) },
                    )
                    Ayuda("«Guardar» te deja elegir la carpeta (Descargas, Drive…). «Compartir» lo envía por WhatsApp, correo, etc.")
                }
            },
            confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        )
    }
    if (ocupado) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Preparando archivo…") },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
    }
}

@Composable
private fun OpcionFormato(titulo: String, descripcion: String, onCompartir: () -> Unit, onGuardar: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(descripcion, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCompartir, modifier = Modifier.weight(1f)) { Text("Compartir") }
                Button(onClick = onGuardar, modifier = Modifier.weight(1f)) { Text("Guardar") }
            }
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.AppViewModel
import com.elprofe.despiece.Pantalla
import com.elprofe.despiece.core.Calculadora
import com.elprofe.despiece.core.DespieceElemento
import com.elprofe.despiece.core.Elemento
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.core.TipoElemento

@Composable
fun PantallaObra(obra: Obra, vm: AppViewModel) {
    val contexto = LocalContext.current
    val despieces = remember(obra) {
        val calc = Calculadora(obra.ajustes)
        obra.elementos.map { calc.calcular(it) }
    }
    var elegirTipo by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf<Elemento?>(null) }
    var exportar by remember { mutableStateOf(false) }

    fun abrirExportacion() {
        if (obra.elementos.isEmpty()) {
            Toast.makeText(contexto, "La obra no tiene elementos todavía", Toast.LENGTH_SHORT).show()
        } else {
            exportar = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(obra.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BotonVolver { vm.volver() } },
                actions = {
                    IconButton(onClick = { vm.ir(Pantalla.AjustesObra(obra.id)) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes de la obra")
                    }
                    IconButton(onClick = { abrirExportacion() }) {
                        Icon(Icons.Filled.Share, contentDescription = "Exportar a Excel o PDF")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { elegirTipo = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Elemento") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    TarjetaTotal(
                        despieces,
                        onResumen = { vm.ir(Pantalla.Resumen(obra.id)) },
                        onExportar = { abrirExportacion() },
                    )
                }
                if (obra.elementos.isEmpty()) {
                    item {
                        Ayuda("Añade losas, vigas, muros y pilares con el botón «Elemento». Cada elemento puede repetirse las veces que haga falta.")
                    }
                }
                items(despieces, key = { it.elemento.id }) { d ->
                    TarjetaElemento(
                        d,
                        onAbrir = { vm.ir(Pantalla.EditarElemento(obra.id, d.elemento.id)) },
                        onDuplicar = { vm.duplicarElemento(obra.id, d.elemento.id) },
                        onEliminar = { eliminar = d.elemento },
                    )
                }
            }
        }
    }

    ExportacionObra(obra, visible = exportar, onCerrar = { exportar = false })
    if (elegirTipo) {
        DialogoTipo(
            onElegir = { tipo ->
                elegirTipo = false
                val id = vm.agregarElemento(obra.id, tipo)
                if (id.isNotEmpty()) vm.ir(Pantalla.EditarElemento(obra.id, id))
            },
            onCancelar = { elegirTipo = false },
        )
    }
    eliminar?.let { e ->
        DialogoConfirmar(
            titulo = "¿Eliminar el elemento?",
            mensaje = "Se borrará «${e.nombre}» de la obra.",
            confirmar = "Eliminar",
            onConfirmar = {
                vm.eliminarElemento(obra.id, e.id)
                eliminar = null
            },
            onCancelar = { eliminar = null },
        )
    }
}

@Composable
private fun TarjetaTotal(despieces: List<DespieceElemento>, onResumen: () -> Unit, onExportar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Peso total de acero", style = MaterialTheme.typography.labelLarge)
            Text(
                "${Formato.kg(despieces.sumOf { it.pesoTotal })} kg",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            val barras = despieces.sumOf { it.barrasPorElemento * it.veces }
            Text("$barras barras cortadas · ${despieces.size} elementos", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(4.dp))
            Button(onClick = onResumen, enabled = despieces.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Resumen por diámetro y plan de corte")
            }
            OutlinedButton(onClick = onExportar, enabled = despieces.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar a Excel o PDF")
            }
        }
    }
}

@Composable
fun InsigniaTipo(tipo: TipoElemento) {
    val color = when (tipo) {
        TipoElemento.LOSA -> MaterialTheme.colorScheme.tertiary
        TipoElemento.VIGA -> MaterialTheme.colorScheme.primary
        TipoElemento.MURO -> MaterialTheme.colorScheme.secondary
        TipoElemento.PILAR -> MaterialTheme.colorScheme.error
    }
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                tipo.inicial,
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TarjetaElemento(
    d: DespieceElemento,
    onAbrir: () -> Unit,
    onDuplicar: () -> Unit,
    onEliminar: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val e = d.elemento
    OutlinedCard(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            InsigniaTipo(e.tipo)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    e.nombre + if (d.veces != 1) "  × ${d.veces}" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(resumenElemento(e), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${Formato.kg(d.pesoTotal)} kg · ${d.barrasPorElemento * d.veces} barras",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (d.notas.any { !it.contains("se corta en") }) {
                    Text("Revisa las notas del despiece", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; onDuplicar() })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onEliminar() })
                }
            }
        }
    }
}

private fun resumenElemento(e: Elemento): String = when (e) {
    is com.elprofe.despiece.core.Losa ->
        "Losa ${Formato.editable(e.largoX)} × ${Formato.editable(e.largoY)} m, e = ${Formato.editable(e.espesor)} cm"
    is com.elprofe.despiece.core.Viga ->
        "Viga ${Formato.editable(e.b)}×${Formato.editable(e.h)} cm, L = ${Formato.editable(e.largo)} m"
    is com.elprofe.despiece.core.Muro ->
        "Muro ${Formato.editable(e.largo)} × ${Formato.editable(e.altura)} m, e = ${Formato.editable(e.espesor)} cm"
    is com.elprofe.despiece.core.Pilar ->
        if (e.circular) "Pilar Ø${Formato.editable(e.diametroSeccion)} cm, H = ${Formato.editable(e.altura)} m"
        else "Pilar ${Formato.editable(e.b)}×${Formato.editable(e.h)} cm, H = ${Formato.editable(e.altura)} m"
}

@Composable
private fun DialogoTipo(onElegir: (TipoElemento) -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Añadir elemento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoElemento.entries.forEach { tipo ->
                    OutlinedCard(onClick = { onElegir(tipo) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            InsigniaTipo(tipo)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(tipo.etiqueta, style = MaterialTheme.typography.titleMedium)
                                Text(tipo.descripcion, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

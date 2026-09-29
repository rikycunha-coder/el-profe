@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.AppViewModel
import com.elprofe.despiece.Pantalla
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.core.pesoTotal
import java.text.DateFormat
import java.util.Date

@Composable
fun PantallaObras(obras: List<Obra>, vm: AppViewModel) {
    var nueva by remember { mutableStateOf(false) }
    var ayuda by remember { mutableStateOf(false) }
    var renombrar by remember { mutableStateOf<Obra?>(null) }
    var eliminar by remember { mutableStateOf<Obra?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Despiece de Hierro") },
                actions = {
                    IconButton(onClick = { ayuda = true }) { Icon(Icons.Filled.Info, contentDescription = "Cómo se calcula") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nueva = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva obra") },
            )
        },
    ) { padding ->
        if (obras.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No hay obras.\nCrea una con «Nueva obra» y añade sus losas, vigas, muros y pilares.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(obras, key = { it.id }) { obra ->
                    TarjetaObra(
                        obra,
                        onAbrir = { vm.ir(Pantalla.DetalleObra(obra.id)) },
                        onRenombrar = { renombrar = obra },
                        onDuplicar = { vm.duplicarObra(obra.id) },
                        onEliminar = { eliminar = obra },
                    )
                }
            }
        }
    }

    if (nueva) {
        DialogoTexto(
            titulo = "Nueva obra",
            etiqueta = "Nombre de la obra",
            inicial = "",
            confirmar = "Crear",
            onConfirmar = {
                nueva = false
                vm.ir(Pantalla.DetalleObra(vm.crearObra(it)))
            },
            onCancelar = { nueva = false },
        )
    }
    renombrar?.let { obra ->
        DialogoTexto(
            titulo = "Renombrar obra",
            etiqueta = "Nombre de la obra",
            inicial = obra.nombre,
            confirmar = "Guardar",
            onConfirmar = {
                vm.renombrarObra(obra.id, it)
                renombrar = null
            },
            onCancelar = { renombrar = null },
        )
    }
    eliminar?.let { obra ->
        DialogoConfirmar(
            titulo = "¿Eliminar la obra?",
            mensaje = "Se borrará «${obra.nombre}» con todos sus elementos. No se puede deshacer.",
            confirmar = "Eliminar",
            onConfirmar = {
                vm.eliminarObra(obra.id)
                eliminar = null
            },
            onCancelar = { eliminar = null },
        )
    }
    if (ayuda) DialogoAyuda { ayuda = false }
}

@Composable
private fun TarjetaObra(
    obra: Obra,
    onAbrir: () -> Unit,
    onRenombrar: () -> Unit,
    onDuplicar: () -> Unit,
    onEliminar: () -> Unit,
) {
    val peso = remember(obra) { obra.pesoTotal() }
    var menu by remember { mutableStateOf(false) }
    Card(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    obra.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val elementos = obra.elementos.size
                Text(
                    "$elementos ${if (elementos == 1) "elemento" else "elementos"} · ${Formato.kg(peso)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (obra.creada > 0) {
                    Text(
                        "Creada el " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(obra.creada)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menu = false; onRenombrar() })
                    DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; onDuplicar() })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onEliminar() })
                }
            }
        }
    }
}

@Composable
private fun DialogoAyuda(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cómo se calcula") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Cada obra usa una norma de referencia (ACI 318 o Eurocódigo 2) que fija patas, ganchos y traslapos. Se cambia en Ajustes (engranaje).")
                Text("• Medidas exteriores de las barras, redondeadas al centímetro. Opcionalmente se descuenta el alargamiento por doblado (2Ø por doblez a 90°).")
                Text("• El recubrimiento se descuenta en cada extremo y en cada cara.")
                Text("• Cantidad de barras = huecos necesarios para no superar la separación + 1.")
                Text("• Si una barra supera el largo comercial (6 o 12 m) se corta en piezas con traslapo.")
                Text("• Peso = π/4 · Ø² · 7.850 kg/m³ (Ø12 = 0,888 kg/m).")
                Text("• El plan de corte agrupa las piezas en barras comerciales dejando el menor sobrante posible. El margen de desperdicio, si lo pides, se muestra aparte del neto.")
                Text(
                    "Los valores por defecto son orientativos: comprueba anclajes, traslapos y ganchos con tu norma y con los planos del calculista.",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } },
    )
}

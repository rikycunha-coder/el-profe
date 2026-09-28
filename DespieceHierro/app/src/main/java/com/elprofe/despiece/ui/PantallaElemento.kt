@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.AppViewModel
import com.elprofe.despiece.core.Calculadora
import com.elprofe.despiece.core.DespieceElemento
import com.elprofe.despiece.core.Elemento
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.Obra

@Composable
fun PantallaElemento(obra: Obra, elemento: Elemento, vm: AppViewModel) {
    val despiece = remember(elemento, obra.ajustes) { Calculadora(obra.ajustes).calcular(elemento) }
    var pestana by rememberSaveable(elemento.id) { mutableIntStateOf(0) }
    var eliminar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(elemento.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${elemento.tipo.etiqueta} · ${obra.nombre}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { BotonVolver { vm.volver() } },
                actions = {
                    IconButton(onClick = { eliminar = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar elemento")
                    }
                },
            )
        },
        bottomBar = { BarraTotal(despiece) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Datos") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Despiece (${despiece.barras.size})") })
            }
            if (pestana == 0) {
                CompositionLocalProvider(LocalAjustes provides obra.ajustes) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        EditorElemento(elemento) { vm.actualizarElemento(obra.id, it) }
                        if (despiece.notas.isNotEmpty()) TarjetaNotas(despiece.notas)
                    }
                }
            } else {
                ListaBarras(despiece, Modifier.fillMaxSize())
            }
        }
    }

    if (eliminar) {
        DialogoConfirmar(
            titulo = "¿Eliminar el elemento?",
            mensaje = "Se borrará «${elemento.nombre}» de la obra.",
            confirmar = "Eliminar",
            onConfirmar = {
                eliminar = false
                vm.volver()
                vm.eliminarElemento(obra.id, elemento.id)
            },
            onCancelar = { eliminar = false },
        )
    }
}

@Composable
private fun BarraTotal(d: DespieceElemento) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (d.veces > 1) "Total de ${d.veces} elementos" else "Total",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text("${Formato.kg(d.pesoTotal)} kg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (d.veces > 1) Text("${Formato.kg(d.pesoPorElemento)} kg c/u", style = MaterialTheme.typography.bodyMedium)
                Text("${d.barrasPorElemento * d.veces} barras", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

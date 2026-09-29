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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.AppViewModel
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.core.ResumenDiametro
import com.elprofe.despiece.core.ResumenObra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_PATRONES = 60

@Composable
fun PantallaResumen(obra: Obra, vm: AppViewModel) {
    val resumen by produceState<ResumenObra?>(initialValue = null, obra) {
        value = withContext(Dispatchers.Default) { ResumenObra.de(obra) }
    }

    var exportar by remember { mutableStateOf(false) }
    ExportacionObra(obra, visible = exportar, onCerrar = { exportar = false })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen · ${obra.nombre}", maxLines = 1) },
                navigationIcon = { BotonVolver { vm.volver() } },
                actions = {
                    IconButton(onClick = { exportar = true }) {
                        Icon(Icons.Filled.Share, contentDescription = "Exportar a Excel o PDF")
                    }
                },
            )
        },
    ) { padding ->
        val r = resumen
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { TarjetaTotales(r) }
                item { TablaDiametros(r) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Plan de corte · barras de ${Formato.editable(obra.ajustes.largoComercial)} m",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Ayuda("Medidas de las piezas en cm. Toca un diámetro para ver cómo cortar cada barra.")
                    }
                }
                items(r.porDiametro, key = { it.diametro }) { TarjetaCorte(it) }
                item { TablaElementos(r) }
            }
        }
    }
}

@Composable
private fun TarjetaTotales(r: ResumenObra) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Acero colocado en obra", style = MaterialTheme.typography.labelLarge)
            Text("${Formato.kg(r.peso)} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "A comprar: ${r.barrasComerciales} barras · ${Formato.kg(r.pesoComprado)} kg",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("Desperdicio de corte: ${Formato.porcentaje(r.desperdicio)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TablaDiametros(r: ResumenObra) {
    Seccion("Por diámetro") {
        val pesos = listOf(0.8f, 1.2f, 1.2f, 1f, 1f)
        FilaTabla(listOf("Ø", "Metros", "Peso kg", "Barras", "Desp."), pesos, encabezado = true)
        HorizontalDivider()
        r.porDiametro.forEach {
            FilaTabla(
                listOf(
                    "Ø${it.diametro}",
                    Formato.num(it.metros, 1),
                    Formato.kg(it.peso),
                    it.barrasComerciales.toString(),
                    Formato.num(it.desperdicio * 100, 0) + "%",
                ),
                pesos,
            )
        }
        HorizontalDivider()
        FilaTabla(
            listOf("Total", "", Formato.kg(r.peso), r.barrasComerciales.toString(), Formato.num(r.desperdicio * 100, 0) + "%"),
            pesos,
            encabezado = true,
        )
        Ayuda("Barras = barras comerciales a comprar según el plan de corte.")
    }
}

@Composable
private fun TarjetaCorte(rd: ResumenDiametro) {
    var abierto by rememberSaveable(rd.diametro) { mutableStateOf(false) }
    OutlinedCard(onClick = { abierto = !abierto }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ø${rd.diametro}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${rd.piezas} piezas → ${rd.barrasComerciales} barras · desperdicio ${Formato.porcentaje(rd.desperdicio)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    if (abierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (abierto) "Ocultar" else "Ver cortes",
                )
            }
            if (abierto) {
                HorizontalDivider()
                rd.plan.patrones.take(MAX_PATRONES).forEach { p ->
                    val barras = p.veces * p.barrasPorPatron
                    Text(
                        "$barras ${if (barras == 1) "barra" else "barras"}: ${p.descripcion()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        if (p.barrasPorPatron > 1) "   pieza más larga que la barra comercial: pedir a medida o soldar"
                        else "   sobra ${p.sobrante} cm por barra",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (rd.plan.patrones.size > MAX_PATRONES) {
                    Ayuda("…y ${rd.plan.patrones.size - MAX_PATRONES} formas de corte más (ver PDF).")
                }
            }
        }
    }
}

@Composable
private fun TablaElementos(r: ResumenObra) {
    Seccion("Por elemento") {
        val pesos = listOf(2f, 0.7f, 1.2f)
        FilaTabla(listOf("Elemento", "Uds", "Peso kg"), pesos, encabezado = true)
        HorizontalDivider()
        r.despieces.forEach {
            FilaTabla(listOf(it.elemento.nombre, it.veces.toString(), Formato.kg(it.pesoTotal)), pesos)
        }
    }
}

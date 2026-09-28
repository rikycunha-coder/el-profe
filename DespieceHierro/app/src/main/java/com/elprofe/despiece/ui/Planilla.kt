@file:OptIn(ExperimentalTextApi::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.core.Alineacion
import com.elprofe.despiece.core.Barra
import com.elprofe.despiece.core.Croquis
import com.elprofe.despiece.core.DespieceElemento
import com.elprofe.despiece.core.Formato
import kotlin.math.min

/** Dibujo de la forma de la barra con sus medidas en cm (no a escala). */
@Composable
fun CroquisBarra(barra: Barra, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val colorBarra = MaterialTheme.colorScheme.primary
    val estilo = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
    val dibujo = remember(barra) { Croquis.de(barra) }
    Canvas(modifier) {
        val escala = min(size.width / Croquis.ANCHO, size.height / Croquis.ALTO)
        val dx = (size.width - Croquis.ANCHO * escala) / 2f
        val dy = (size.height - Croquis.ALTO * escala) / 2f
        fun punto(x: Float, y: Float) = Offset(dx + x * escala, dy + y * escala)
        val trazo = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        for (linea in dibujo.lineas) {
            val path = Path()
            linea.forEachIndexed { i, p ->
                val o = punto(p.x, p.y)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, colorBarra, style = trazo)
        }
        for (c in dibujo.circulos) {
            drawCircle(colorBarra, radius = c.radio * escala, center = punto(c.centro.x, c.centro.y), style = trazo)
        }
        for (e in dibujo.etiquetas) {
            val texto = medidor.measure(e.texto, estilo)
            val o = punto(e.posicion.x, e.posicion.y)
            val x = when (e.alineacion) {
                Alineacion.IZQUIERDA -> o.x
                Alineacion.CENTRO -> o.x - texto.size.width / 2f
                Alineacion.DERECHA -> o.x - texto.size.width
            }
            drawText(texto, topLeft = Offset(x, o.y - texto.size.height / 2f))
        }
    }
}

@Composable
fun FilaBarra(barra: Barra, veces: Int) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${barra.marca}. ${barra.descripcion}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Ø${barra.diametro} · ${barra.forma.etiqueta}", style = MaterialTheme.typography.bodyMedium)
                Text("Medidas: ${barra.medidas()} cm", style = MaterialTheme.typography.bodySmall)
                val total = barra.cantidad * veces
                val cantidad = if (veces > 1) "${barra.cantidad} × $veces = $total uds" else "$total uds"
                Text(
                    "L = ${Formato.metros(barra.longitud)} m · $cantidad · ${Formato.kg(barra.pesoTotal * veces)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CroquisBarra(barra, Modifier.size(width = 120.dp, height = 56.dp))
        }
    }
}

@Composable
fun TarjetaNotas(notas: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                notas.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** Fila de tabla simple con columnas proporcionales. */
@Composable
fun FilaTabla(celdas: List<String>, pesos: List<Float>, encabezado: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        celdas.forEachIndexed { i, c ->
            Text(
                c,
                modifier = Modifier.weight(pesos[i]),
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                style = if (encabezado) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (encabezado) FontWeight.SemiBold else FontWeight.Normal,
                color = if (encabezado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun ListaBarras(despiece: DespieceElemento, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (despiece.notas.isNotEmpty()) item { TarjetaNotas(despiece.notas) }
        if (despiece.barras.isEmpty()) {
            item { Ayuda("Todavía no hay barras. Completa los datos del elemento.") }
        } else {
            item {
                Ayuda(
                    if (despiece.veces > 1) "Cantidades por elemento × ${despiece.veces} elementos iguales. Medidas en cm."
                    else "Medidas en cm, exteriores. Largo de corte de cada barra en m.",
                )
            }
        }
        items(despiece.barras, key = { it.marca }) { FilaBarra(it, despiece.veces) }
        if (despiece.barras.isNotEmpty()) {
            item {
                Seccion("Totales por diámetro") {
                    val pesos = listOf(1f, 1.3f, 1.3f)
                    FilaTabla(listOf("Ø", "Metros", "Peso (kg)"), pesos, encabezado = true)
                    HorizontalDivider()
                    despiece.porDiametro().forEach {
                        FilaTabla(listOf("Ø${it.diametro}", Formato.num(it.metros), Formato.kg(it.kg)), pesos)
                    }
                    HorizontalDivider()
                    FilaTabla(listOf("Total", "", Formato.kg(despiece.pesoTotal)), pesos, encabezado = true)
                }
            }
        }
    }
}

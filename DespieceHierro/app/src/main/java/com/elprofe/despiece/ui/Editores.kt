@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.core.BarraExtra
import com.elprofe.despiece.core.Elemento
import com.elprofe.despiece.core.Estribos
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.FormaExtra
import com.elprofe.despiece.core.GrupoBarras
import com.elprofe.despiece.core.Losa
import com.elprofe.despiece.core.Malla
import com.elprofe.despiece.core.Muro
import com.elprofe.despiece.core.Pilar
import com.elprofe.despiece.core.Refuerzo
import com.elprofe.despiece.core.Viga
import com.elprofe.despiece.core.con

@Composable
fun EditorElemento(e: Elemento, onCambio: (Elemento) -> Unit) {
    Seccion("General") {
        CampoTexto("Nombre o eje (ej. V-101)", e.nombre) { onCambio(e.con(nombre = it)) }
        CampoEntero("Cantidad de elementos iguales", e.cantidad, unidad = "uds") { onCambio(e.con(cantidad = it)) }
    }
    when (e) {
        is Losa -> EditorLosa(e, onCambio)
        is Viga -> EditorViga(e, onCambio)
        is Muro -> EditorMuro(e, onCambio)
        is Pilar -> EditorPilar(e, onCambio)
    }
    EditorExtras(e.extras) { onCambio(e.con(extras = it)) }
}

// ------------------------------------------------------------------ Losa

@Composable
private fun EditorLosa(e: Losa, onCambio: (Elemento) -> Unit) {
    Seccion("Geometría") {
        Fila {
            CampoNumero("Largo X", e.largoX, "m", Modifier.weight(1f)) { onCambio(e.copy(largoX = it)) }
            CampoNumero("Largo Y", e.largoY, "m", Modifier.weight(1f)) { onCambio(e.copy(largoY = it)) }
        }
        Fila {
            CampoNumero("Espesor", e.espesor, "cm", Modifier.weight(1f)) { onCambio(e.copy(espesor = it)) }
            CampoNumero("Recubrimiento", e.recubrimiento, "cm", Modifier.weight(1f)) { onCambio(e.copy(recubrimiento = it)) }
        }
        Ayuda("Paño rectangular medido entre bordes exteriores. Para losas en L o con varios paños, crea un elemento por paño.")
    }
    Seccion("Malla inferior") {
        EditorMalla(e.inferior) { onCambio(e.copy(inferior = it)) }
    }
    Seccion("Malla superior") {
        Interruptor("Colocar malla superior", e.conSuperior, "Para suples solo sobre apoyos usa «Barras adicionales».") {
            onCambio(e.copy(conSuperior = it))
        }
        if (e.conSuperior) EditorMalla(e.superior) { onCambio(e.copy(superior = it)) }
    }
}

@Composable
private fun EditorMalla(m: Malla, onCambio: (Malla) -> Unit) {
    Subtitulo("Barras en dirección X (miden el largo X)")
    Fila {
        SelectorDiametro("Diámetro", m.diametroX, Modifier.weight(1f)) { onCambio(m.copy(diametroX = it)) }
        CampoNumero("Separación", m.separacionX, "cm", Modifier.weight(1f)) { onCambio(m.copy(separacionX = it)) }
    }
    CampoPata("Patas en los extremos", m.pataX, m.diametroX) { onCambio(m.copy(pataX = it)) }
    Subtitulo("Barras en dirección Y (miden el largo Y)")
    Fila {
        SelectorDiametro("Diámetro", m.diametroY, Modifier.weight(1f)) { onCambio(m.copy(diametroY = it)) }
        CampoNumero("Separación", m.separacionY, "cm", Modifier.weight(1f)) { onCambio(m.copy(separacionY = it)) }
    }
    CampoPata("Patas en los extremos", m.pataY, m.diametroY) { onCambio(m.copy(pataY = it)) }
}

// ------------------------------------------------------------------ Viga

@Composable
private fun EditorViga(e: Viga, onCambio: (Elemento) -> Unit) {
    Seccion("Geometría") {
        Fila {
            CampoNumero("Ancho b", e.b, "cm", Modifier.weight(1f)) { onCambio(e.copy(b = it)) }
            CampoNumero("Alto h", e.h, "cm", Modifier.weight(1f)) { onCambio(e.copy(h = it)) }
        }
        Fila {
            CampoNumero("Largo total", e.largo, "m", Modifier.weight(1f)) { onCambio(e.copy(largo = it)) }
            CampoNumero("Recubrimiento", e.recubrimiento, "cm", Modifier.weight(1f)) { onCambio(e.copy(recubrimiento = it)) }
        }
        Ayuda("Largo total entre caras exteriores de los apoyos extremos. Las barras corridas lo recorren entero.")
    }
    Seccion("Armadura inferior") {
        EditorGrupo(e.inferior) { onCambio(e.copy(inferior = it)) }
    }
    Seccion("Armadura superior") {
        EditorGrupo(e.superior) { onCambio(e.copy(superior = it)) }
    }
    Seccion("Armadura de piel (laterales)") {
        EditorGrupo(e.piel) { onCambio(e.copy(piel = it)) }
        Ayuda("Número total de barras en ambas caras. 0 = sin armadura de piel.")
    }
    Seccion("Estribos") {
        EditorEstribos(e.estribos) { onCambio(e.copy(estribos = it)) }
        Fila {
            CampoEntero("Vanos iguales", e.tramos, Modifier.weight(1f)) { onCambio(e.copy(tramos = it)) }
            CampoEntero("Trabas por estribo", e.trabas, Modifier.weight(1f)) { onCambio(e.copy(trabas = it)) }
        }
        CampoOpcional(
            etiqueta = "Largo con estribos",
            valor = e.largoEstribado,
            unidad = "m",
            textoAutomatico = "Vacío = largo total (${Formato.editable(e.largo)} m)",
            ayudaConValor = "Suma de las luces libres entre apoyos",
        ) { onCambio(e.copy(largoEstribado = it)) }
        Ayuda("Con varios vanos, el largo con estribos se reparte en partes iguales y cada vano lleva sus zonas de confinamiento.")
    }
}

@Composable
private fun EditorGrupo(g: GrupoBarras, onCambio: (GrupoBarras) -> Unit) {
    Fila {
        CampoEntero("Cantidad", g.cantidad, Modifier.weight(1f), unidad = "uds") { onCambio(g.copy(cantidad = it)) }
        SelectorDiametro("Diámetro", g.diametro, Modifier.weight(1f)) { onCambio(g.copy(diametro = it)) }
    }
    CampoPata("Patas en los extremos", g.pata, g.diametro) { onCambio(g.copy(pata = it)) }
}

@Composable
private fun EditorEstribos(est: Estribos, onCambio: (Estribos) -> Unit) {
    Fila {
        SelectorDiametro("Diámetro", est.diametro, Modifier.weight(1f)) { onCambio(est.copy(diametro = it)) }
        CampoNumero("Separación", est.separacion, "cm", Modifier.weight(1f)) { onCambio(est.copy(separacion = it)) }
    }
    Subtitulo("Zona de confinamiento (en cada extremo)")
    Fila {
        CampoNumero("Largo zona", est.largoZona, "cm", Modifier.weight(1f)) { onCambio(est.copy(largoZona = it)) }
        CampoNumero("Separación zona", est.separacionZona, "cm", Modifier.weight(1f)) { onCambio(est.copy(separacionZona = it)) }
    }
    Ayuda("Largo de zona 0 = estribos a separación constante.")
}

// ------------------------------------------------------------------ Muro

@Composable
private fun EditorMuro(e: Muro, onCambio: (Elemento) -> Unit) {
    Seccion("Geometría") {
        Fila {
            CampoNumero("Largo", e.largo, "m", Modifier.weight(1f)) { onCambio(e.copy(largo = it)) }
            CampoNumero("Altura", e.altura, "m", Modifier.weight(1f)) { onCambio(e.copy(altura = it)) }
        }
        Fila {
            CampoNumero("Espesor", e.espesor, "cm", Modifier.weight(1f)) { onCambio(e.copy(espesor = it)) }
            CampoNumero("Recubrimiento", e.recubrimiento, "cm", Modifier.weight(1f)) { onCambio(e.copy(recubrimiento = it)) }
        }
        Interruptor("Doble malla (armadura en las dos caras)", e.dobleMalla) { onCambio(e.copy(dobleMalla = it)) }
    }
    Seccion("Barras verticales") {
        EditorRefuerzo(e.vertical) { onCambio(e.copy(vertical = it)) }
        CampoNumero("Anclaje en la fundación", e.anclaje, "cm", ayuda = "Largo recto por debajo del arranque del muro") {
            onCambio(e.copy(anclaje = it))
        }
        CampoPata("Pata inferior", e.pataInferior, e.vertical.diametro) { onCambio(e.copy(pataInferior = it)) }
        Interruptor(
            "Dejar traslapo arriba (espera)",
            e.traslapoSuperior,
            "Suma ${LocalAjustes.current.empalme(e.vertical.diametro)} cm para empalmar con el nivel superior",
        ) { onCambio(e.copy(traslapoSuperior = it)) }
    }
    Seccion("Barras horizontales") {
        EditorRefuerzo(e.horizontal) { onCambio(e.copy(horizontal = it)) }
        CampoPata("Patas en los extremos", e.pataHorizontal, e.horizontal.diametro) { onCambio(e.copy(pataHorizontal = it)) }
    }
    if (e.dobleMalla) {
        Seccion("Trabas entre mallas") {
            Fila {
                SelectorDiametro("Diámetro", e.diametroTrabas, Modifier.weight(1f)) { onCambio(e.copy(diametroTrabas = it)) }
                CampoNumero("Trabas por m²", e.trabasPorM2, "uds", Modifier.weight(1f)) { onCambio(e.copy(trabasPorM2 = it)) }
            }
            Ayuda("0 = sin trabas.")
        }
    }
}

@Composable
private fun EditorRefuerzo(r: Refuerzo, onCambio: (Refuerzo) -> Unit) {
    Fila {
        SelectorDiametro("Diámetro", r.diametro, Modifier.weight(1f)) { onCambio(r.copy(diametro = it)) }
        CampoNumero("Separación", r.separacion, "cm", Modifier.weight(1f)) { onCambio(r.copy(separacion = it)) }
    }
}

// ------------------------------------------------------------------ Pilar

@Composable
private fun EditorPilar(e: Pilar, onCambio: (Elemento) -> Unit) {
    Seccion("Geometría") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !e.circular, onClick = { onCambio(e.copy(circular = false)) }, label = { Text("Rectangular") })
            FilterChip(selected = e.circular, onClick = { onCambio(e.copy(circular = true)) }, label = { Text("Circular") })
        }
        if (e.circular) {
            CampoNumero("Diámetro de la sección", e.diametroSeccion, "cm") { onCambio(e.copy(diametroSeccion = it)) }
        } else {
            Fila {
                CampoNumero("Lado b", e.b, "cm", Modifier.weight(1f)) { onCambio(e.copy(b = it)) }
                CampoNumero("Lado h", e.h, "cm", Modifier.weight(1f)) { onCambio(e.copy(h = it)) }
            }
        }
        Fila {
            CampoNumero("Altura", e.altura, "m", Modifier.weight(1f)) { onCambio(e.copy(altura = it)) }
            CampoNumero("Recubrimiento", e.recubrimiento, "cm", Modifier.weight(1f)) { onCambio(e.copy(recubrimiento = it)) }
        }
    }
    Seccion("Armadura longitudinal") {
        Fila {
            CampoEntero("Cantidad", e.longitudinal.cantidad, Modifier.weight(1f), unidad = "uds") {
                onCambio(e.copy(longitudinal = e.longitudinal.copy(cantidad = it)))
            }
            SelectorDiametro("Diámetro", e.longitudinal.diametro, Modifier.weight(1f)) {
                onCambio(e.copy(longitudinal = e.longitudinal.copy(diametro = it)))
            }
        }
        CampoNumero("Anclaje en la fundación", e.anclaje, "cm", ayuda = "Largo recto por debajo del arranque del pilar") {
            onCambio(e.copy(anclaje = it))
        }
        CampoPata("Pata inferior", e.longitudinal.pata, e.longitudinal.diametro) {
            onCambio(e.copy(longitudinal = e.longitudinal.copy(pata = it)))
        }
        Interruptor(
            "Dejar traslapo arriba (espera)",
            e.traslapoSuperior,
            "Suma ${LocalAjustes.current.empalme(e.longitudinal.diametro)} cm para empalmar con el nivel superior",
        ) { onCambio(e.copy(traslapoSuperior = it)) }
    }
    Seccion(if (e.circular) "Zunchos (estribos circulares)" else "Estribos") {
        EditorEstribos(e.estribos) { onCambio(e.copy(estribos = it)) }
        if (!e.circular) {
            Subtitulo("Trabas por nivel de estribo")
            Fila {
                CampoEntero("Paralelas a b", e.trabasB, Modifier.weight(1f)) { onCambio(e.copy(trabasB = it)) }
                CampoEntero("Paralelas a h", e.trabasH, Modifier.weight(1f)) { onCambio(e.copy(trabasH = it)) }
            }
        }
    }
}

// ------------------------------------------------------------------ Barras adicionales

@Composable
private fun EditorExtras(extras: List<BarraExtra>, onCambio: (List<BarraExtra>) -> Unit) {
    Seccion("Barras adicionales") {
        Ayuda("Suples, bastones, esquineros, refuerzos de vanos, estribos especiales… Medidas en cm.")
        extras.forEachIndexed { i, x ->
            key(x.id) {
                EditorExtra(i + 1, x, onCambio = { nuevo -> onCambio(extras.map { if (it.id == x.id) nuevo else it }) }) {
                    onCambio(extras.filterNot { it.id == x.id })
                }
            }
        }
        OutlinedButton(onClick = { onCambio(extras + BarraExtra()) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir barra")
        }
    }
}

@Composable
private fun EditorExtra(numero: Int, x: BarraExtra, onCambio: (BarraExtra) -> Unit, onQuitar: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CampoTexto("Descripción", x.descripcion.ifEmpty { "" }, Modifier.weight(1f)) { onCambio(x.copy(descripcion = it)) }
                IconButton(onClick = onQuitar) { Icon(Icons.Filled.Delete, contentDescription = "Quitar barra $numero") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormaExtra.entries.forEach { f ->
                    FilterChip(selected = x.forma == f, onClick = { onCambio(x.copy(forma = f)) }, label = { Text(f.etiqueta) })
                }
            }
            Fila {
                CampoEntero("Cantidad", x.cantidad, Modifier.weight(1f), unidad = "uds") { onCambio(x.copy(cantidad = it)) }
                SelectorDiametro("Diámetro", x.diametro, Modifier.weight(1f)) { onCambio(x.copy(diametro = it)) }
            }
            when (x.forma) {
                FormaExtra.BARRA -> {
                    CampoNumero("Largo recto", x.largo, "cm") { onCambio(x.copy(largo = it)) }
                    Fila {
                        CampoNumero("Pata inicio", x.pataInicio, "cm", Modifier.weight(1f)) { onCambio(x.copy(pataInicio = it)) }
                        CampoNumero("Pata fin", x.pataFin, "cm", Modifier.weight(1f)) { onCambio(x.copy(pataFin = it)) }
                    }
                }
                FormaExtra.ESTRIBO -> Fila {
                    CampoNumero("Ancho a", x.largo, "cm", Modifier.weight(1f)) { onCambio(x.copy(largo = it)) }
                    CampoNumero("Alto b", x.alto, "cm", Modifier.weight(1f)) { onCambio(x.copy(alto = it)) }
                }
            }
            Ayuda("Barra: recta, con una pata (L) o con dos (U). Cantidad por elemento; sin descripción se llamará «Adicional $numero».")
        }
    }
}

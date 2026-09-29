@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.AppViewModel
import com.elprofe.despiece.core.Ajustes
import com.elprofe.despiece.core.Formato
import com.elprofe.despiece.core.Norma
import com.elprofe.despiece.core.Obra

@Composable
fun PantallaAjustes(obra: Obra, vm: AppViewModel) {
    val aj = obra.ajustes
    fun cambiar(nuevo: Ajustes) = vm.cambiarAjustes(obra.id, nuevo)

    /** Cambiar a mano un largo de la norma pasa la obra a «criterio propio». */
    fun cambiarFactor(igual: Boolean, nuevo: () -> Ajustes) {
        if (!igual) cambiar(nuevo().copy(norma = Norma.PROPIA))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes de la obra") },
                navigationIcon = { BotonVolver { vm.volver() } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Seccion("Norma y material") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = aj.norma == Norma.ACI_318,
                        onClick = { cambiar(aj.conNorma(Norma.ACI_318)) },
                        label = { Text("ACI 318") },
                    )
                    FilterChip(
                        selected = aj.norma == Norma.EUROCODIGO_2,
                        onClick = { cambiar(aj.conNorma(Norma.EUROCODIGO_2)) },
                        label = { Text("Eurocódigo 2") },
                    )
                    FilterChip(
                        selected = aj.norma == Norma.PROPIA,
                        onClick = { cambiar(aj.copy(norma = Norma.PROPIA)) },
                        label = { Text("Propio") },
                    )
                }
                Ayuda(aj.norma.detalle)
                CampoTexto("Material (tipo de acero)", aj.acero) { cambiar(aj.copy(acero = it)) }
                Ayuda("Ej.: B500S, A630-420H, grado 60. Aparece en el Excel y el PDF.")
            }
            Seccion("Barra comercial") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6.0, 12.0).forEach { largo ->
                        FilterChip(
                            selected = aj.largoComercial == largo,
                            onClick = { cambiar(aj.copy(largoComercial = largo)) },
                            label = { Text("${Formato.editable(largo)} m") },
                        )
                    }
                }
                CampoNumero("Largo de la barra comercial", aj.largoComercial, "m") {
                    if (it >= 1.0) cambiar(aj.copy(largoComercial = it))
                }
                Ayuda("Las barras más largas se cortan en piezas con traslapo. También se usa para el plan de corte.")
            }
            Seccion("Traslapos") {
                CampoNumero("Traslapo hasta Ø20", aj.empalmeDiametros, "× Ø") {
                    cambiarFactor(it == aj.empalmeDiametros) { aj.copy(empalmeDiametros = it) }
                }
                CampoOpcional(
                    etiqueta = "Traslapo desde Ø${Ajustes.DIAMETRO_GRUESO}",
                    valor = aj.empalmeGruesoDiametros,
                    unidad = "× Ø",
                    textoAutomatico = "Vacío = igual que hasta Ø20",
                    ayudaConValor = "Vacío = igual que hasta Ø20",
                ) { cambiarFactor(it == aj.empalmeGruesoDiametros) { aj.copy(empalmeGruesoDiametros = it) } }
                Ayuda(
                    "Ejemplos: Ø10 = ${aj.empalme(10)} cm · Ø12 = ${aj.empalme(12)} cm · Ø16 = ${aj.empalme(16)} cm · " +
                        "Ø25 = ${aj.empalme(25)} cm. Se usa en las esperas de muros y pilares y al empalmar barras largas.",
                )
            }
            Seccion("Patas, ganchos y doblado") {
                CampoNumero("Pata automática", aj.pataDiametros, "× Ø") {
                    cambiarFactor(it == aj.pataDiametros) { aj.copy(pataDiametros = it) }
                }
                Ayuda("Se aplica cuando el campo de pata de un elemento queda vacío. Ø12 = ${aj.pataAutomatica(12)} cm.")
                CampoNumero("Gancho de estribos y trabas", aj.ganchoDiametros, "× Ø") {
                    cambiarFactor(it == aj.ganchoDiametros) { aj.copy(ganchoDiametros = it) }
                }
                Ayuda(
                    "Largo de cada gancho, mínimo ${Formato.editable(Ajustes.GANCHO_MINIMO_CM)} cm. " +
                        "Ø8 = ${aj.gancho(8)} cm · Ø10 = ${aj.gancho(10)} cm.",
                )
                Interruptor(
                    "Descontar alargamiento por doblado",
                    aj.descontarDoblado,
                    "Resta 2Ø por cada doblez a 90° (patas y esquinas de estribos). " +
                        "Apagado: medidas exteriores, resultado algo conservador.",
                ) { cambiar(aj.copy(descontarDoblado = it)) }
            }
            Seccion("Margen de seguridad") {
                CampoNumero("Desperdicio adicional", aj.margenDesperdicio, "%") {
                    if (it <= 50.0) cambiar(aj.copy(margenDesperdicio = it))
                }
                Ayuda(
                    "Opcional, normalmente del 3 al 5 %. Solo se suma a la lista de compra y se muestra aparte del neto; " +
                        "0 = sin margen.",
                )
            }
            OutlinedButton(onClick = { cambiar(Ajustes.de(Norma.ACI_318)) }) { Text("Restablecer valores de ACI 318") }
            Ayuda(
                "Los valores de cada norma son orientativos (acero y hormigón habituales). Comprueba anclajes, traslapos " +
                    "y ganchos con los planos del calculista.",
            )
        }
    }
}

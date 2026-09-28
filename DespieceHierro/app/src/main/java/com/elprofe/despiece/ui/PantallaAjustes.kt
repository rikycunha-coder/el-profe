@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.elprofe.despiece.core.Obra

@Composable
fun PantallaAjustes(obra: Obra, vm: AppViewModel) {
    val aj = obra.ajustes
    fun cambiar(nuevo: Ajustes) = vm.cambiarAjustes(obra.id, nuevo)

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
            Seccion("Barras comerciales") {
                CampoNumero("Largo de la barra comercial", aj.largoComercial, "m") {
                    if (it >= 1.0) cambiar(aj.copy(largoComercial = it))
                }
                Ayuda("Normalmente 12 m. Se usa para cortar barras largas y para el plan de corte.")
            }
            Seccion("Traslapos") {
                CampoNumero("Largo de traslapo", aj.empalmeDiametros, "× Ø") { cambiar(aj.copy(empalmeDiametros = it)) }
                Ayuda(
                    "Ejemplos: Ø10 = ${aj.empalme(10)} cm · Ø12 = ${aj.empalme(12)} cm · Ø16 = ${aj.empalme(16)} cm. " +
                        "Se usa en las esperas de muros y pilares y al empalmar barras más largas que la comercial.",
                )
            }
            Seccion("Patas y ganchos") {
                CampoNumero("Pata automática", aj.pataDiametros, "× Ø") { cambiar(aj.copy(pataDiametros = it)) }
                Ayuda("Se aplica cuando el campo de pata de un elemento queda vacío. Ø12 = ${aj.pataAutomatica(12)} cm.")
                CampoNumero("Gancho de estribos y trabas", aj.ganchoDiametros, "× Ø") { cambiar(aj.copy(ganchoDiametros = it)) }
                Ayuda(
                    "Largo de cada gancho, mínimo ${Formato.editable(Ajustes.GANCHO_MINIMO_CM)} cm. " +
                        "Ø8 = ${aj.gancho(8)} cm · Ø10 = ${aj.gancho(10)} cm.",
                )
            }
            OutlinedButton(onClick = { cambiar(Ajustes()) }) { Text("Restablecer valores por defecto") }
            Ayuda(
                "Los valores por defecto son orientativos. Comprueba anclajes, traslapos y ganchos con la norma " +
                    "que aplique en tu país (ACI 318 / NCh 430, Código Estructural / EHE, CIRSOC 201, NSR-10, E.060…) " +
                    "y con los planos del calculista.",
            )
        }
    }
}

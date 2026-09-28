package com.elprofe.despiece.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.elprofe.despiece.AppViewModel
import com.elprofe.despiece.Pantalla
import com.elprofe.despiece.core.Obra

@Composable
fun App(vm: AppViewModel) {
    val obras by vm.obras.collectAsState()
    BackHandler(enabled = vm.puedeVolver) { vm.volver() }

    Surface(Modifier.fillMaxSize()) {
        if (vm.cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Navegacion(obras, vm)
        }
    }
}

@Composable
private fun Navegacion(obras: List<Obra>, vm: AppViewModel) {
    when (val p = vm.pantalla) {
        Pantalla.Obras -> PantallaObras(obras, vm)
        is Pantalla.DetalleObra -> {
            val obra = obras.find { it.id == p.obraId }
            if (obra != null) PantallaObra(obra, vm) else Volver(vm)
        }
        is Pantalla.EditarElemento -> {
            val obra = obras.find { it.id == p.obraId }
            val elemento = obra?.elementos?.find { it.id == p.elementoId }
            if (obra != null && elemento != null) PantallaElemento(obra, elemento, vm) else Volver(vm)
        }
        is Pantalla.Resumen -> {
            val obra = obras.find { it.id == p.obraId }
            if (obra != null) PantallaResumen(obra, vm) else Volver(vm)
        }
        is Pantalla.AjustesObra -> {
            val obra = obras.find { it.id == p.obraId }
            if (obra != null) PantallaAjustes(obra, vm) else Volver(vm)
        }
    }
}

/** La obra o el elemento ya no existe (se borró): vuelve a la pantalla anterior. */
@Composable
private fun Volver(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.volver() }
}

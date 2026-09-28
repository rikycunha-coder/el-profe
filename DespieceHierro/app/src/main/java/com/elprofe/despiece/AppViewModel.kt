package com.elprofe.despiece

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elprofe.despiece.core.Ajustes
import com.elprofe.despiece.core.Almacen
import com.elprofe.despiece.core.Elemento
import com.elprofe.despiece.core.Obra
import com.elprofe.despiece.core.TipoElemento
import com.elprofe.despiece.core.con
import com.elprofe.despiece.core.nuevoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Pantalla {
    data object Obras : Pantalla
    data class DetalleObra(val obraId: String) : Pantalla
    data class EditarElemento(val obraId: String, val elementoId: String) : Pantalla
    data class Resumen(val obraId: String) : Pantalla
    data class AjustesObra(val obraId: String) : Pantalla
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = Repositorio(app)
    private val _obras = MutableStateFlow<List<Obra>>(emptyList())
    val obras: StateFlow<List<Obra>> = _obras.asStateFlow()

    var cargando by mutableStateOf(true)
        private set

    private val pila = mutableStateListOf<Pantalla>(Pantalla.Obras)
    val pantalla: Pantalla get() = pila.last()
    val puedeVolver: Boolean get() = pila.size > 1

    /** Cola de guardado: solo se escribe la última versión pendiente. */
    private val porGuardar = Channel<List<Obra>>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            val guardadas = withContext(Dispatchers.IO) { repositorio.cargar() }
            _obras.value = guardadas ?: listOf(Almacen.obraDeEjemplo(System.currentTimeMillis()))
            if (guardadas == null) porGuardar.trySend(_obras.value)
            cargando = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            for (obras in porGuardar) repositorio.guardar(obras)
        }
    }

    fun ir(p: Pantalla) {
        pila.add(p)
    }

    fun volver() {
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
    }

    private fun cambiar(f: (List<Obra>) -> List<Obra>) {
        _obras.update(f)
        porGuardar.trySend(_obras.value)
    }

    private fun cambiarObra(id: String, f: (Obra) -> Obra) =
        cambiar { lista -> lista.map { if (it.id == id) f(it) else it } }

    // ------------------------------------------------------------ Obras

    fun crearObra(nombre: String): String {
        val obra = Obra(nombre = nombre.trim().ifEmpty { "Obra nueva" }, creada = System.currentTimeMillis())
        cambiar { listOf(obra) + it }
        return obra.id
    }

    fun renombrarObra(id: String, nombre: String) =
        cambiarObra(id) { it.copy(nombre = nombre.trim().ifEmpty { it.nombre }) }

    fun duplicarObra(id: String) {
        val original = _obras.value.find { it.id == id } ?: return
        val copia = original.copy(
            id = nuevoId(),
            nombre = original.nombre + " (copia)",
            creada = System.currentTimeMillis(),
            elementos = original.elementos.map { it.con(id = nuevoId()) },
        )
        cambiar { lista ->
            val i = lista.indexOfFirst { it.id == id }
            lista.toMutableList().apply { add(i + 1, copia) }
        }
    }

    fun eliminarObra(id: String) = cambiar { lista -> lista.filterNot { it.id == id } }

    fun cambiarAjustes(obraId: String, ajustes: Ajustes) = cambiarObra(obraId) { it.copy(ajustes = ajustes) }

    // ------------------------------------------------------------ Elementos

    fun agregarElemento(obraId: String, tipo: TipoElemento): String {
        val obra = _obras.value.find { it.id == obraId } ?: return ""
        val patron = Regex("^${tipo.etiqueta} (\\d+)$")
        val siguiente = (obra.elementos.mapNotNull { patron.find(it.nombre)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0) + 1
        val elemento = tipo.crear("${tipo.etiqueta} $siguiente")
        cambiarObra(obraId) { it.copy(elementos = it.elementos + elemento) }
        return elemento.id
    }

    fun actualizarElemento(obraId: String, elemento: Elemento) = cambiarObra(obraId) { obra ->
        obra.copy(elementos = obra.elementos.map { if (it.id == elemento.id) elemento else it })
    }

    fun duplicarElemento(obraId: String, elementoId: String) = cambiarObra(obraId) { obra ->
        val i = obra.elementos.indexOfFirst { it.id == elementoId }
        if (i < 0) return@cambiarObra obra
        val original = obra.elementos[i]
        val copia = original.con(id = nuevoId(), nombre = original.nombre + " (copia)")
        obra.copy(elementos = obra.elementos.toMutableList().apply { add(i + 1, copia) })
    }

    fun eliminarElemento(obraId: String, elementoId: String) = cambiarObra(obraId) { obra ->
        obra.copy(elementos = obra.elementos.filterNot { it.id == elementoId })
    }
}

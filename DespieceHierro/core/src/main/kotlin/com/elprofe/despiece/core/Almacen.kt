package com.elprofe.despiece.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class Archivo(
    val version: Int = 1,
    val obras: List<Obra> = emptyList(),
)

/** Convierte las obras a JSON y de vuelta, para guardarlas en el teléfono. */
object Almacen {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "clase"
    }

    fun codificar(obras: List<Obra>): String = json.encodeToString(Archivo.serializer(), Archivo(obras = obras))

    fun decodificar(texto: String): List<Obra> = json.decodeFromString(Archivo.serializer(), texto).obras

    /** Obra de ejemplo para el primer arranque, con un elemento de cada tipo. */
    fun obraDeEjemplo(ahora: Long): Obra = Obra(
        nombre = "Ejemplo · Vivienda",
        creada = ahora,
        ajustes = Ajustes.de(Norma.ACI_318),
        elementos = listOf(
            Losa(nombre = "Losa L-1", largoX = 5.2, largoY = 4.1, conSuperior = true),
            Viga(nombre = "Viga V-1", largo = 5.4, cantidad = 2),
            Muro(nombre = "Muro M-1", largo = 6.0, altura = 2.4, trabasPorM2 = 4.0),
            Pilar(nombre = "Pilar P-1", cantidad = 4, altura = 2.6),
        ),
    )
}

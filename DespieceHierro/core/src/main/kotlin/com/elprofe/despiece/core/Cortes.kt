package com.elprofe.despiece.core

import java.util.TreeMap

/** Una forma de cortar una barra comercial, repetida [veces]. */
data class PatronCorte(
    /** Piezas en cm, de mayor a menor. */
    val piezas: List<Int>,
    val veces: Int,
    /** Barras comerciales por patrón (más de 1 solo si una pieza supera la barra comercial). */
    val barrasPorPatron: Int,
    /** Sobrante por patrón, cm. */
    val sobrante: Int,
) {
    /** "2 × 480 + 230" */
    fun descripcion(): String {
        val partes = mutableListOf<String>()
        var i = 0
        while (i < piezas.size) {
            var j = i
            while (j < piezas.size && piezas[j] == piezas[i]) j++
            val n = j - i
            partes += if (n > 1) "$n × ${piezas[i]}" else "${piezas[i]}"
            i = j
        }
        return partes.joinToString(" + ")
    }
}

class PlanCorte(
    val diametro: Int,
    /** cm */
    val largoComercial: Int,
    val patrones: List<PatronCorte>,
) {
    val barras: Int = patrones.sumOf { it.veces * it.barrasPorPatron }
    val piezas: Int = patrones.sumOf { it.veces * it.piezas.size }
    /** cm de barra que se usan en obra. */
    val largoUtil: Long = patrones.sumOf { it.veces.toLong() * it.piezas.sum() }
    /** cm de barra que hay que comprar. */
    val largoComprado: Long = barras.toLong() * largoComercial
    val desperdicio: Double = if (largoComprado > 0) 1.0 - largoUtil.toDouble() / largoComprado else 0.0
}

/**
 * Optimización de cortes con el método "mejor ajuste decreciente": se cortan primero
 * las piezas largas y cada pieza se saca de la barra empezada donde deja menos sobrante.
 */
object Optimizador {

    private class BarraEnCorte(val barras: Int, var libre: Int) {
        val piezas = mutableListOf<Int>()
    }

    /** [piezas]: largo en cm → número de piezas. */
    fun planificar(diametro: Int, piezas: Map<Int, Int>, largoComercial: Int): PlanCorte {
        val abiertas = TreeMap<Int, ArrayDeque<BarraEnCorte>>()
        val usadas = mutableListOf<BarraEnCorte>()

        for ((largo, cantidad) in piezas.entries.sortedByDescending { it.key }) {
            if (largo <= 0 || cantidad <= 0) continue
            if (largo > largoComercial) {
                // Pieza más larga que la barra comercial: se pide soldada o a medida.
                val n = divArriba(largo, largoComercial)
                repeat(cantidad) {
                    usadas += BarraEnCorte(n, n * largoComercial - largo).also { it.piezas += largo }
                }
                continue
            }
            repeat(cantidad) {
                val clave = abiertas.ceilingKey(largo)
                val barra = if (clave != null) {
                    val cola = abiertas.getValue(clave)
                    cola.removeFirst().also { if (cola.isEmpty()) abiertas.remove(clave) }
                } else {
                    BarraEnCorte(1, largoComercial).also { usadas += it }
                }
                barra.piezas += largo
                barra.libre -= largo
                if (barra.libre > 0) abiertas.getOrPut(barra.libre) { ArrayDeque() }.addLast(barra)
            }
        }

        val patrones = usadas
            .groupingBy { it.piezas.toList() to it.barras }
            .eachCount()
            .map { (clave, veces) ->
                val (lista, barras) = clave
                PatronCorte(lista, veces, barras, barras * largoComercial - lista.sum())
            }
            .sortedWith(compareByDescending<PatronCorte> { it.veces }.thenByDescending { it.piezas.first() })
        return PlanCorte(diametro, largoComercial, patrones)
    }
}

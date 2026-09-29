package com.elprofe.despiece.core

import kotlin.math.PI
import kotlin.math.ceil

/**
 * Genera la planilla de despiece de cada elemento.
 *
 * Convenciones:
 *  - Las medidas de las barras son exteriores y se redondean al centímetro.
 *  - El recubrimiento se descuenta en cada extremo y en cada cara.
 *  - Una barra más larga que la barra comercial se divide en piezas con traslapo.
 *  - No se descuentan alargamientos por doblado (resultado ligeramente conservador).
 */
class Calculadora(private val ajustes: Ajustes) {

    private val largoComercial = ajustes.largoComercialCm

    fun calcular(elemento: Elemento): DespieceElemento {
        val planilla = Planilla()
        when (elemento) {
            is Losa -> losa(elemento, planilla)
            is Viga -> viga(elemento, planilla)
            is Muro -> muro(elemento, planilla)
            is Pilar -> pilar(elemento, planilla)
        }
        extras(elemento.extras, planilla)
        if (elemento.cantidad <= 0) planilla.nota("La cantidad de elementos iguales es 0: no suma peso.")
        return DespieceElemento(elemento, planilla.barras(), planilla.notas)
    }

    // ---------------------------------------------------------------- Losa

    private fun losa(e: Losa, p: Planilla) {
        val r = e.recubrimiento
        val lx = e.largoX * 100
        val ly = e.largoY * 100
        if (lx <= 2 * r || ly <= 2 * r) {
            p.nota("Los largos de la losa deben ser mayores que dos recubrimientos.")
            return
        }
        if (e.espesor <= 2 * r) p.nota("El espesor es menor o igual que dos recubrimientos.")
        malla(p, "Inferior", e.inferior, lx, ly, r)
        if (e.conSuperior) malla(p, "Superior", e.superior, lx, ly, r)
    }

    private fun malla(p: Planilla, capa: String, m: Malla, lx: Double, ly: Double, r: Double) {
        if (separacionValida(p, "$capa dir. X", m.separacionX)) {
            val n = porSeparacion(ly - 2 * r, m.separacionX)
            val pata = ajustes.pata(m.pataX, m.diametroX)
            longitudinal(p, "$capa dir. X", m.diametroX, n, redondear(lx - 2 * r), pata, pata)
        }
        if (separacionValida(p, "$capa dir. Y", m.separacionY)) {
            val n = porSeparacion(lx - 2 * r, m.separacionY)
            val pata = ajustes.pata(m.pataY, m.diametroY)
            longitudinal(p, "$capa dir. Y", m.diametroY, n, redondear(ly - 2 * r), pata, pata)
        }
    }

    // ---------------------------------------------------------------- Viga

    private fun viga(e: Viga, p: Planilla) {
        val r = e.recubrimiento
        val recto = redondear(e.largo * 100 - 2 * r)
        grupo(p, "Inferior", e.inferior, recto)
        grupo(p, "Superior", e.superior, recto)
        grupo(p, "Piel (laterales)", e.piel, recto)

        val a = redondear(e.b - 2 * r)
        val h = redondear(e.h - 2 * r)
        val est = e.estribos
        if (a <= 0 || h <= 0) {
            p.nota("La sección es demasiado pequeña para el recubrimiento indicado.")
            return
        }
        if (!separacionValida(p, "Estribos", est.separacion)) return
        val tramos = maxOf(1, e.tramos)
        val largoEstribado = (e.largoEstribado ?: e.largo) * 100
        val n = tramos * estribosConZonas(largoEstribado / tramos, est.separacion, est.largoZona, est.separacionZona)
        estribo(p, "Estribos", est.diametro, n, a, h)
        if (e.trabas > 0) traba(p, "Trabas", est.diametro, n * e.trabas, a)
    }

    // ---------------------------------------------------------------- Muro

    private fun muro(e: Muro, p: Planilla) {
        val r = e.recubrimiento
        val largo = e.largo * 100
        val altura = e.altura * 100
        if (largo <= 2 * r || altura <= 0) {
            p.nota("El largo y la altura del muro deben ser mayores que cero.")
            return
        }
        val caras = if (e.dobleMalla) 2 else 1

        val dv = e.vertical.diametro
        if (separacionValida(p, "Verticales", e.vertical.separacion)) {
            val n = porSeparacion(largo - 2 * r, e.vertical.separacion) * caras
            val arriba = if (e.traslapoSuperior) ajustes.empalme(dv).toDouble() else -r
            val recto = redondear(e.anclaje + altura + arriba)
            longitudinal(p, "Verticales", dv, n, recto, ajustes.pata(e.pataInferior, dv), 0)
        }

        val dh = e.horizontal.diametro
        if (separacionValida(p, "Horizontales", e.horizontal.separacion)) {
            val n = porSeparacion(altura - 2 * r, e.horizontal.separacion) * caras
            val pata = ajustes.pata(e.pataHorizontal, dh)
            longitudinal(p, "Horizontales", dh, n, redondear(largo - 2 * r), pata, pata)
        }

        if (e.dobleMalla && e.trabasPorM2 > 0) {
            val n = ceil(e.largo * e.altura * e.trabasPorM2 - 1e-6).toInt()
            val a = redondear(e.espesor - 2 * r)
            if (a > 0) traba(p, "Trabas", e.diametroTrabas, n, a)
            else p.nota("El espesor del muro no deja espacio para trabas.")
        }
    }

    // ---------------------------------------------------------------- Pilar

    private fun pilar(e: Pilar, p: Planilla) {
        val r = e.recubrimiento
        val altura = e.altura * 100
        if (altura <= 0) {
            p.nota("La altura del pilar debe ser mayor que cero.")
            return
        }
        verticalPilar(p, "Longitudinales", e.longitudinal, e, altura)
        if (!e.circular) verticalPilar(p, "Piel (caras)", e.piel, e, altura)

        val est = e.estribos
        if (!separacionValida(p, "Estribos", est.separacion)) return
        val n = estribosConZonas(altura, est.separacion, est.largoZona, est.separacionZona)
        if (e.circular) {
            val d = redondear(e.diametroSeccion - 2 * r)
            if (d <= 0) p.nota("La sección es demasiado pequeña para el recubrimiento indicado.")
            else circulo(p, "Zunchos", est.diametro, n, d)
        } else {
            val a = redondear(e.b - 2 * r)
            val h = redondear(e.h - 2 * r)
            if (a <= 0 || h <= 0) {
                p.nota("La sección es demasiado pequeña para el recubrimiento indicado.")
                return
            }
            estribo(p, "Estribos", est.diametro, n, a, h)
            if (e.trabasB > 0) traba(p, "Trabas paralelas a b", est.diametro, n * e.trabasB, a)
            if (e.trabasH > 0) traba(p, "Trabas paralelas a h", est.diametro, n * e.trabasH, h)
        }
    }

    /** Barras verticales del pilar: anclaje en la fundación, pata abajo y espera con traslapo arriba. */
    private fun verticalPilar(p: Planilla, desc: String, g: GrupoBarras, e: Pilar, altura: Double) {
        if (g.cantidad <= 0) return
        val arriba = if (e.traslapoSuperior) ajustes.empalme(g.diametro).toDouble() else -e.recubrimiento
        val recto = redondear(e.anclaje + altura + arriba)
        longitudinal(p, desc, g.diametro, g.cantidad, recto, ajustes.pata(g.pata, g.diametro), 0)
    }

    // ---------------------------------------------------------------- Barras adicionales

    private fun extras(lista: List<BarraExtra>, p: Planilla) {
        lista.forEachIndexed { i, x ->
            if (x.cantidad <= 0) return@forEachIndexed
            val desc = x.descripcion.trim().ifEmpty { "Adicional ${i + 1}" }
            when (x.forma) {
                FormaExtra.BARRA -> longitudinal(
                    p, desc, x.diametro, x.cantidad,
                    redondear(x.largo), redondear(x.pataInicio), redondear(x.pataFin),
                )
                FormaExtra.ESTRIBO -> {
                    val a = redondear(x.largo)
                    val h = redondear(x.alto)
                    if (a > 0 && h > 0) estribo(p, desc, x.diametro, x.cantidad, a, h)
                    else p.nota("$desc: el ancho y el alto del estribo deben ser mayores que cero.")
                }
            }
        }
    }

    // ---------------------------------------------------------------- Piezas

    private fun grupo(p: Planilla, desc: String, g: GrupoBarras, recto: Int) {
        if (g.cantidad <= 0) return
        val pata = ajustes.pata(g.pata, g.diametro)
        longitudinal(p, desc, g.diametro, g.cantidad, recto, pata, pata)
    }

    private fun separacionValida(p: Planilla, desc: String, separacion: Double): Boolean {
        if (separacion >= SEPARACION_MINIMA) return true
        p.nota("$desc: la separación debe ser de al menos ${Formato.editable(SEPARACION_MINIMA)} cm.")
        return false
    }

    /**
     * Barra recta con patas opcionales en cada extremo. Si supera la barra comercial,
     * se corta en piezas unidas con traslapo: la primera lleva la pata inicial y la
     * última la pata final.
     */
    private fun longitudinal(p: Planilla, desc: String, d: Int, cantidad: Int, recto: Int, pataIni: Int, pataFin: Int) {
        if (cantidad <= 0) return
        if (recto <= 0) {
            p.nota("$desc: el largo resultante es nulo; revisa las medidas.")
            return
        }
        val total = pataIni + recto + pataFin
        if (total <= largoComercial) {
            p.agregar(conPatas(desc, d, cantidad, pataIni, recto, pataFin))
            return
        }
        val empalme = ajustes.empalme(d)
        if (2 * empalme + maxOf(pataIni, pataFin) >= largoComercial) {
            p.nota("$desc: mide ${Formato.metros(total)} m, más que la barra comercial, y el traslapo es demasiado largo para dividirla.")
            p.agregar(conPatas(desc, d, cantidad, pataIni, recto, pataFin))
            return
        }
        // Cada pieza aporta (largo comercial − traslapo) salvo la última.
        val k = divArriba(total - empalme, largoComercial - empalme)
        val piezas = List(k) { i ->
            intArrayOf(if (i == 0) pataIni else 0, 0, if (i == k - 1) pataFin else 0)
        }
        for (i in 0 until k - 1) piezas[i][1] = largoComercial - piezas[i][0]
        val ultimo = total + (k - 1) * empalme - (k - 1) * largoComercial
        piezas[k - 1][1] = ultimo - pataFin
        if (piezas[k - 1][1] < empalme) {
            // La última pieza debe tener al menos el traslapo en recto: se lo quita a la anterior.
            val falta = empalme - piezas[k - 1][1]
            piezas[k - 1][1] += falta
            piezas[k - 2][1] -= falta
        }
        p.nota("$desc: mide ${Formato.metros(total)} m; se corta en $k piezas con traslapo de $empalme cm.")

        // Agrupa las piezas iguales (normalmente las intermedias).
        val grupos = LinkedHashMap<List<Int>, MutableList<Int>>()
        piezas.forEachIndexed { i, pz -> grupos.getOrPut(pz.toList()) { mutableListOf() } += i + 1 }
        for ((pz, indices) in grupos) {
            val nombre = when {
                indices.size == 1 -> "$desc · pieza ${indices[0]} de $k"
                indices.last() - indices.first() + 1 == indices.size ->
                    "$desc · piezas ${indices.first()}–${indices.last()} de $k"
                else -> "$desc · piezas ${indices.joinToString(", ")} de $k"
            }
            p.agregar(conPatas(nombre, d, cantidad * indices.size, pz[0], pz[1], pz[2]))
        }
    }

    private fun conPatas(desc: String, d: Int, cantidad: Int, pataIni: Int, recto: Int, pataFin: Int): Barra {
        val largo = pataIni + recto + pataFin
        return when {
            pataIni > 0 && pataFin > 0 ->
                Barra(0, desc, d, Forma.U, listOf(pataIni, recto, pataFin), 0, largo, cantidad)
            pataIni > 0 || pataFin > 0 ->
                Barra(0, desc, d, Forma.L, listOf(maxOf(pataIni, pataFin), recto), 0, largo, cantidad)
            else -> Barra(0, desc, d, Forma.RECTA, listOf(recto), 0, largo, cantidad)
        }
    }

    private fun estribo(p: Planilla, desc: String, d: Int, cantidad: Int, a: Int, h: Int) {
        val g = ajustes.gancho(d)
        p.agregar(Barra(0, desc, d, Forma.ESTRIBO, listOf(a, h), g, 2 * a + 2 * h + 2 * g, cantidad))
    }

    private fun traba(p: Planilla, desc: String, d: Int, cantidad: Int, a: Int) {
        val g = ajustes.gancho(d)
        p.agregar(Barra(0, desc, d, Forma.TRABA, listOf(a), g, a + 2 * g, cantidad))
    }

    private fun circulo(p: Planilla, desc: String, d: Int, cantidad: Int, diametro: Int) {
        val g = ajustes.gancho(d)
        p.agregar(Barra(0, desc, d, Forma.CIRCULO, listOf(diametro), g, redondearArriba(PI * diametro) + 2 * g, cantidad))
    }

    /** Acumula filas, sumando las que son idénticas, y numera las marcas al final. */
    private class Planilla {
        private data class Clave(val desc: String, val d: Int, val forma: Forma, val tramos: List<Int>, val gancho: Int)

        private val filas = LinkedHashMap<Clave, Barra>()
        val notas = mutableListOf<String>()

        fun agregar(b: Barra) {
            if (b.cantidad <= 0) return
            val clave = Clave(b.descripcion, b.diametro, b.forma, b.tramos, b.gancho)
            val previa = filas[clave]
            filas[clave] = previa?.copy(cantidad = previa.cantidad + b.cantidad) ?: b
        }

        fun nota(texto: String) {
            notas += texto
        }

        fun barras(): List<Barra> = filas.values.mapIndexed { i, b -> b.copy(marca = i + 1) }
    }

    companion object {
        const val SEPARACION_MINIMA = 3.0
    }
}

fun Obra.pesoTotal(): Double {
    val calc = Calculadora(ajustes)
    return elementos.sumOf { calc.calcular(it).pesoTotal }
}
